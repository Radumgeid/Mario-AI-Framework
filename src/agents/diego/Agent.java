package agents.diego;

//Importamos el core de Mario AI Framework y la libreria Jess
import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;
import jess.*;
import java.util.*;

public class Agent implements MarioAgent {	
	//Accion de salida:   action[] = [left, right, down, speed, jump]
	private boolean action[] = new boolean[MarioActions.numberOfActions()];
	//Rete es el nucleo de Jess, el motor de inferencia 
	private Rete engine;
	private Fact factMario;
	//Inicializacion del agente (quieto) y del SBC
    public void initialize(MarioForwardModel model, MarioTimer timer) {
    	//Creacion del sistema de reglas
    	{try {
    		engine = new Rete();
            engine.reset();    
            //Importamos los templates y las reglas del SBC
            engine.batch("E:/Escritorio/Mario-AI-Framework-master/src/agents/diego/jessModel.clp");
            //Introducimos instancia de Mario con los valores por defecto (quieto y puede saltar)
            factMario = new Fact("Mario", engine);
            engine.assertFact(factMario);
        } catch (JessException ex) {
            System.err.println(ex);
        }}
    	
    	//Agente quieto
    	action = new boolean[MarioActions.numberOfActions()];
		action[MarioActions.LEFT.getValue()] = false;
		action[MarioActions.RIGHT.getValue()] = false;
		action[MarioActions.DOWN.getValue()] = false;
		action[MarioActions.SPEED.getValue()] = false;
		action[MarioActions.JUMP.getValue()] = false;
    }
    

    /**
     * get mario current actions
     * @param model a forward model object so the agent can simulate the future.
     * @param timer amount of time before the agent has to return the actions.
     * @return an array of the state of the buttons on the controller
     */
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer){
    	
    	{try{
    	//Actualizamos el estado de Mario en la base de hechos (¿puede saltar o seguir saltando?)
    	if(model.mayMarioJump() || model.getMarioCanJumpHigher()){		///////////////////// || model.getMarioCanJumpHigher()
    		engine.modify(factMario, "puedeSaltar", new Value(1, RU.INTEGER));
    	} else{
    		engine.modify(factMario, "puedeSaltar", new Value(0, RU.INTEGER));
    	}
    	//Actualizamos el estado de Mario en la base de hechos (¿se esta moviendo?)
    	if(model.getMarioFloatVelocity()[0] != 0){
    		engine.modify(factMario, "moviendose", new Value(1, RU.INTEGER));
    	} else{
    		engine.modify(factMario, "moviendose", new Value(0, RU.INTEGER));
    	}
    	//Actualizamos el estado de Mario en la base de hechos (¿esta en el suelo?)
    	try{
    		// 0 es aire, y 63 es el tallo intangible de las plataformas seta
	    	if(model.getScreenSceneObservation(0)[model.getMarioScreenTilePos()[0]][model.getMarioScreenTilePos()[1]+1] != 0 &&
	    	model.getScreenSceneObservation(0)[model.getMarioScreenTilePos()[0]][model.getMarioScreenTilePos()[1]+1] != 63){
	    		engine.modify(factMario, "enSuelo", new Value(1, RU.INTEGER));
	    	} else{
	    		engine.modify(factMario, "enSuelo", new Value(0, RU.INTEGER));
	    	}
    	} catch (ArrayIndexOutOfBoundsException ex) {
            System.err.println(ex);
        }
    	
    		
    	//Eliminamos los enemigos anteriores de la base de hechos (distancias desactualizadas tras realizar la accion)
    	engine.removeFacts("enemigoDelante");
		//Comprobamos si hay enemigos a la vista
    	for(int i = 0; i < model.getEnemiesFloatPos().length; i = i + 3){
        	//Introducimos al enemigo visualizado en la base de hechos
            Fact factEnemigo = new Fact("enemigoDelante", engine);
            //La distancia sera la coordenada x o y de Mario - la coordenada x o y del enemigo
            float distanciaX = model.getEnemiesFloatPos()[i+1]-model.getMarioFloatPos()[0];
            float distanciaY = model.getEnemiesFloatPos()[i+2]-model.getMarioFloatPos()[1];
            factEnemigo.setSlotValue("distanciaX", new Value(distanciaX, RU.INTEGER));
            factEnemigo.setSlotValue("distanciaY", new Value(distanciaY, RU.INTEGER));
            float tipo = model.getEnemiesFloatPos()[i];
            factEnemigo.setSlotValue("tipo", new Value(tipo, RU.INTEGER));
            engine.assertFact(factEnemigo);
        }
    	

        //Eliminamos los obstaculos anteriores de la base de hechos (casillas de distancia desactualizadas tras realizar la accion)
        engine.removeFacts("obstaculoDelante");
        //La posicion inmediatamente delante de Mario es [9, 8], desde ahi comprobaremos las siguientes casillas horizontalmente
        //Iteramos hasta el borde la matriz de observacion (que tiene tamaño 16x16)
        int posicionHorizontal = 9;
        int posicionVertical;
        boolean obsEncontrado = false;
        while(posicionHorizontal < 16 && obsEncontrado != true){
        	//Si no hay un espacio en blanco, es un obstaculo a superar
        	if(model.getMarioSceneObservation()[posicionHorizontal][8] != 0 && model.getMarioSceneObservation()[posicionHorizontal][8] != 31){
        		//Introducimos el obstaculo en la base de hechos
        		Fact factObstaculo = new Fact("obstaculoDelante", engine);
        		factObstaculo.setSlotValue("distancia", new Value((posicionHorizontal-8), RU.INTEGER));
        		//Introducimos si hay un peligro en las primeras casillas encima del obstaculo
        		posicionVertical = 8;
        		while(model.getMarioSceneObservation()[posicionHorizontal][posicionVertical] != 0 && posicionVertical >= 1){
	        		if(model.getMarioSceneObservation()[posicionHorizontal][posicionVertical-1] == 0 && model.getMarioEnemiesObservation()[posicionHorizontal][posicionVertical-1] != 0){
	        			factObstaculo.setSlotValue("peligroDeSalto", new Value(1, RU.INTEGER));
	        		}
	        		//Si se puede, comprobamos siguiente casilla de encima (por las tuberias, principalmente)
	        		else if(posicionHorizontal != 15){
	        			if(model.getMarioSceneObservation()[posicionHorizontal][posicionVertical-1] == 0 && model.getMarioEnemiesObservation()[posicionHorizontal+1][posicionVertical-1] != 0){
		        			factObstaculo.setSlotValue("peligroDeSalto", new Value(1, RU.INTEGER));
		        		}
	        		}
	        		posicionVertical--;
        		}
        		
        		engine.assertFact(factObstaculo);
        		obsEncontrado=true;
        	}
        	posicionHorizontal++;
        }
        
        /*
        //Imprimir matriz de observacion
        for(int i = 0; i<model.obsGridWidth; i++){
			for(int j = 0; j<model.obsGridHeight; j++){
				if(model.getMarioScreenTilePos()[0] == j && model.getMarioScreenTilePos()[1] == i){
					System.out.print("M  ");
				}
				else{
					System.out.print(model.getScreenSceneObservation(0)[j][i] + "  ");
				}
			}
			System.out.println();
		}
        // 
        */
        
        try{
	        //Eliminamos los hoyos anteriores de la base de hechos (casillas de distancia desactualizadas tras realizar la accion)
	        engine.removeFacts("caidaDelante");
	        //La ultima posicion inmediatamente debajo de Mario es [8, 15], desde ahi comprobaremos las siguientes casillas horizontalmente
	        //Iteramos hasta el borde la matriz de observacion (que tiene tamaño 16x16)
	        boolean hayHoyo = false;
	        int x = model.getMarioScreenTilePos()[0];
	        while (x < 16 && hayHoyo == false){
	    		//¿Hay caida desde la altura de Mario hasta el hoyo?
	    		hayHoyo = true;
	    		//Empezamos inmediatamente encima de la casilla mas baja con vision
	    		int alturaAuxiliar = 15;
	    		//El limite es la altura (o profundidad) de Mario
	    		while(hayHoyo != false && alturaAuxiliar > model.getMarioScreenTilePos()[1]){
	    			//No solo 0 es que no hay obstaculo, tambien lo es 63 (cuerpo de las setas del nivel 3)
	    			if(model.getScreenCompleteObservation(0,2)[x][alturaAuxiliar] != 0 && model.getScreenCompleteObservation(0,2)[x][alturaAuxiliar] != 63){
	    				hayHoyo = false;
	    			}
	    			alturaAuxiliar--;
	    		}
	    		if(hayHoyo){
	        		//Calculamos el tamaño del hoyo
	        		int distancia = x - 8;
	        		//Parametro para indicar cuantas casillas verticales quedan para llegar al final del hoyo, util cuando esta en el aire
	        		int caidaHastaFinal = -1;
	        		boolean finHoyo = false;
	        		//Ya hemos contado la primera columna
	        		int tamaño = 1;
	        		x++;
	        		while(x < 15 && finHoyo == false){
	        			alturaAuxiliar = 15;
	        			//Comprobamos cada columna de casillas delante de Mario, si no encontramos espacio, como muy alto, 
	        			//a tres casillas por encima de Mario (lo que puede saltar), o el final del mapa, es final de hoyo
	        			while(alturaAuxiliar >= model.getMarioScreenTilePos()[1] && finHoyo == false){
	        				if(model.getScreenCompleteObservation(0,2)[x][alturaAuxiliar] != 0  && 
	        				model.getScreenCompleteObservation(0,2)[x][alturaAuxiliar] != 63 &&
    						model.getScreenCompleteObservation(0,2)[x][alturaAuxiliar-1] == 0){
	            				finHoyo = true;
	            				caidaHastaFinal = alturaAuxiliar - model.getMarioScreenTilePos()[1];
	            			}
	            			alturaAuxiliar--;
	        			}
	        			if(finHoyo == false){
	        				tamaño++;
	        				x++;
	        			}
	        		}
	        		//Introducimos el hoyo en la base de hechos
	        		Fact factHoyo = new Fact("caidaDelante", engine);
	        		factHoyo.setSlotValue("distancia", new Value(distancia, RU.INTEGER));
	        		factHoyo.setSlotValue("tamano", new Value(tamaño, RU.INTEGER));
	        		factHoyo.setSlotValue("caida", new Value(caidaHastaFinal, RU.INTEGER));
	        		engine.assertFact(factHoyo);
	    		}
	    		x++;
	        }
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.err.println(ex);
        }
        
        try{
        //Eliminamos las plataformas anteriores
        engine.removeFacts("plataformaDelante");
        //Comprobaremos si hay alguna casilla no aire superior a Mario y horizontalmente posterior
        boolean hayPlataforma = false;
        posicionHorizontal = model.getMarioScreenTilePos()[0]+1;
        while(posicionHorizontal < 16 && hayPlataforma == false){
        	posicionVertical = model.getMarioScreenTilePos()[1];
        	//Para que sea una plataforma al menos tiene que tener una casilla de aire debajo
        	if(model.getScreenSceneObservation(0)[posicionHorizontal][model.getMarioScreenTilePos()[1]] == 0 
        	|| model.getScreenSceneObservation(0)[posicionHorizontal][model.getMarioScreenTilePos()[1]] == 63){
	        	posicionVertical--;
	        	while(posicionVertical >= 0 && posicionVertical >= model.getMarioScreenTilePos()[1]-4 && hayPlataforma == false){
	        		//Si vamos hacia arriba y encontramos casilla sin aire, la comprobamos
	        		if(model.getScreenSceneObservation(0)[posicionHorizontal][posicionVertical] != 0
	        		&& model.getScreenSceneObservation(0)[posicionHorizontal][posicionVertical] != 63){
	        			hayPlataforma = true;
	        			//Introducimos la plataforma en la base de hechos
	            		Fact factPlataforma = new Fact("plataformaDelante", engine);
	            		factPlataforma.setSlotValue("distanciaX", new Value(posicionHorizontal-model.getMarioScreenTilePos()[0], RU.INTEGER));
	            		factPlataforma.setSlotValue("distanciaY", new Value(model.getMarioScreenTilePos()[1]-posicionVertical, RU.INTEGER));
	            		// Comprobamos si hay enemigos en las primeras casillas de encima de la plataforma
	            		int hayPeligro = 0;
	            		for(int x = posicionHorizontal-1; x < 16 && hayPeligro != 1 && x < posicionHorizontal+4 ; x++){
		        			if(model.getScreenEnemiesObservation(0)[x][posicionVertical-1] != 0){
		        				hayPeligro = 1;
		        				factPlataforma.setSlotValue("peligroDeSalto", new Value(1, RU.INTEGER));
		        			}
	            		}
	            		engine.assertFact(factPlataforma);
	        		}
	        		posicionVertical--;
	        	}
        	}
        	posicionHorizontal++;
        }
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.err.println(ex);
        }
        
        
        
        
        ///COMPROBADOR DE LA BASE DE HECHOS
        System.out.println("Para la siguiente base de hechos:");
        Iterator<?> iterador = engine.listFacts();
        while (iterador.hasNext()) {
        	System.out.println(iterador.next());
        }
        ///
        
        
        //Activamos el motor de inferencia para que ejecute un maximo de 1 regla
        Activation siguienteRegla = engine.peekNextActivation();
        int nReglas = engine.run(1);
        if (nReglas == 1){
	        System.out.println("Se ejecuta una regla:");
	        System.out.println(siguienteRegla);
        } else{
        	System.out.println("No se ejecuta una regla");
        }
        System.out.println("----------");
        
        //Obtenemos la salida traduciendo el fact de la base de hechos a los datos de accion que realizara Mario
        Context contexto = engine.getGlobalContext();
		action[MarioActions.LEFT.getValue()] = (factMario.getSlotValue("izquierda").intValue(contexto) == 1);
		action[MarioActions.RIGHT.getValue()] = (factMario.getSlotValue("derecha").intValue(contexto) == 1);
		action[MarioActions.DOWN.getValue()] = (factMario.getSlotValue("agachado").intValue(contexto) == 1);
		action[MarioActions.SPEED.getValue()] = (factMario.getSlotValue("corriendo").intValue(contexto) == 1);
		action[MarioActions.JUMP.getValue()] = (factMario.getSlotValue("saltando").intValue(contexto) == 1);
    	 } catch (JessException ex) {
             System.err.println(ex);
         }}
        
    	return action;
    }
    
    //Devuelve el nombre del agente
    public String getAgentName(){
    	return "AgenteDiego";
    }
    
}