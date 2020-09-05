;; Definicion de templates para el modelo

(deftemplate Mario				;; [left, right, down, speed, jump]
	(slot izquierda (type INTEGER) (allowed-values 0 1) (default 0))
	(slot derecha (type INTEGER) (allowed-values 0 1) (default 0))
	(slot agachado (type INTEGER) (allowed-values 0 1) (default 0))
	(slot corriendo (type INTEGER) (allowed-values 0 1) (default 0))
	(slot saltando (type INTEGER) (allowed-values 0 1) (default 0))
	(slot puedeSaltar (type INTEGER) (allowed-values 0 1) (default 1))	;; Indicativo de si Mario podra saltar o llegar mas alto (1) o, por el contrario, no puede saltar (0)
	(slot moviendose (type INTEGER) (allowed-values 0 1) (default 0))	;; Indicativo de si la velocidad en el eje x no es igual a 0
	(slot enSuelo (type INTEGER) (allowed-values 0 1) (default 1))
)

(deftemplate enemigoDelante
	(slot distanciaX (type INTEGER))	;; La distancia horizontal desde Mario al enemigo, -1 indicara que lo hemos dejado atras
	(slot distanciaY (type INTEGER))	;; La distancia vertical desde Mario al enemigo, si es la misma significa que estan en el mismo piso
	(slot tipo (type INTEGER))
)

(deftemplate obstaculoDelante
	(slot distancia (type INTEGER))	;; La distancia horizontal medida en casillas desde Mario al obstaculo
	(slot peligroDeSalto (type INTEGER) (allowed-values 0 1) (default 0))	;; Si no hay peligro al saltar encima del obstaculo (0) o si lo hay (1)
)

(deftemplate caidaDelante
	(slot distancia (type INTEGER))	;; La distancia horizontal medida en casillas desde Mario a un hoyo (donde no hay nada en la casilla mas baja). 
									;; Se admite distancia 0 (caida debajo)
	(slot tamano (type INTEGER))	;; Cuantas casillas de hoyo hay que superar
	(slot caida (type INTEGER))	;; Cuantas casillas de caida hasta el siguiente suelo
)

(deftemplate plataformaDelante
	(slot distanciaX (type INTEGER))	;; La distancia horizontal medida en casillas desde Mario a la plataforma
	(slot distanciaY (type INTEGER))	;; La distancia vertical medida en casillas desde Mario a la plataforma
	(slot peligroDeSalto (type INTEGER) (allowed-values 0 1) (default 0))	;; Si no hay peligro al saltar encima de la plataforma (0) o si lo hay (1)
)

;; Definicion de reglas para el modelo

;;Ley básica: si no puedes saltar pero lo estas intentando, deja de intentar saltar
(defrule dejarDeSaltar (declare (salience 20))
	?m <- (Mario {saltando == 1} {puedeSaltar == 0} {enSuelo == 1})
	=>
	(modify ?m (saltando 0))
)

;; Si no hay nada delante y no hay hoyo, deja de saltar 
(defrule pararSalto (declare (salience 10))
	?m <- (Mario {saltando == 1})
	(not(obstaculoDelante {distancia < 2}))
	(not(caidaDelante {distancia <= 1}))
	=>
	(modify ?m (saltando 0))
)

(defrule avanzar (declare (salience 20))
	?m <- (Mario {derecha == 0})
	=>
	(modify ?m (derecha 1) (izquierda 0))
)

;; Si encuentra un enemigo delante (elegido cerca como 50 de distancia), salta
(defrule saltarEnemigo (declare (salience 20))
	?m <- (Mario {puedeSaltar == 1})
	?e <- (enemigoDelante (distanciaX ?dx) {distanciaX >= 0} {distanciaX < 50} {distanciaY == 0})
	=>
	(modify ?m (saltando 1))
)

;; Si encuentra un enemigo delante que no puede derrotar, lo evita saltando por encima
(defrule evitarEnemigo (declare (salience 25))
	?m <- (Mario {puedeSaltar == 1})
	?e <- (enemigoDelante (distanciaX ?dx) {distanciaX >= 0} {distanciaX < 50} {distanciaY >= 0} {distanciaY <= 40} {tipo == 8})
	=>
	(modify ?m (saltando 1))
)

;; Si encuentra un obstaculo delante (elegido cerca como 1 casilla de distancia) lo salta, siempre que no haya peligro encima
(defrule saltarObstaculo (declare (salience 25))
	?m <- (Mario {puedeSaltar == 1})
	?o <- (obstaculoDelante (distancia ?d) {distancia == 1} {peligroDeSalto == 0})
	=>
	(modify ?m (saltando 1))
)

;; Cuando hay un peligro inminente ligeramente superior, tras no haberlo detectado como obstaculo con peligro encima, retrocede, con misma prioridad que avanzar y 
;; menos que saltar un obstaculo sin peligro, para evitar atrancos en plantas carnivoras
(defrule evitarEnemigoSuperiorInminente (declare (salience 20))
	?m <- (Mario)
	?e <- (enemigoDelante (distanciaX ?dx) {distanciaX >= 0} {distanciaX < 40} {distanciaY >= -30} {distanciaY < -1})
	=>
	(modify ?m (derecha 0) (izquierda 1))
)

;; Si hay un hoyo y una plataforma delante con peligro, espera a que no haya peligro
(defrule esperarAPlataformaSegura (declare (salience 25))
	?m <- (Mario {saltando == 0} {enSuelo == 1})
	?p <- (plataformaDelante (distanciaX ?dx) {distanciaY <= 4} {peligroDeSalto == 1})
	(caidaDelante {distancia >= 0} {distancia < 3})
	=>
	(modify ?m (derecha 0) (izquierda 0))
)

;; Si hay un hoyo y una plataforma delante con peligro, espera a que no haya peligro, regula tu velocidad si es necesario
(defrule regularVelocidad (declare (salience 25))
	?m <- (Mario {moviendose == 1} {saltando == 0} {enSuelo == 1})
	?p <- (plataformaDelante (distanciaX ?dx) {distanciaY <= 4} {peligroDeSalto == 1})
	(caidaDelante {distancia >= 0} {distancia < 3})
	=>
	(modify ?m (derecha 0) (izquierda 1))
)


;; Si encuentra un hoyo delante (elegido cerca como 1 casilla de distancia) o esta encima de uno, lo salta
(defrule saltarHoyo (declare (salience 15))
	?m <- (Mario {puedeSaltar == 1})
	?h <- (caidaDelante (distancia ?d) {distancia >= 0} {distancia < 2})
	=>
	(modify ?m (saltando 1))
)

;; Si encuentra un hoyo delante (elegido cerca como 1 casilla de distancia) o esta encima de uno, lo salta
(defrule cogerImpulso (declare (salience 25))
	?m <- (Mario {puedeSaltar == 1})
	?h <- (caidaDelante (distancia ?d) {distancia >= 0} {distancia < 2})
	?p <- (plataformaDelante (distanciaX ?dx) {distanciaY <= 4} (distanciaY ?dy) {peligroDeSalto == 1})
	(test (>= ?dx (* ?dy 2)))
	=>
	(modify ?m (derecha 0) (izquierda 1))
)

;; Si estamos sobre un hoyo que no podemos superar, hay que buscar suelo atras
(defrule buscarAterrizaje (declare (salience 20))
	?m <- (Mario {saltando == 0} {puedeSaltar == 0} {enSuelo == 0})
	(caidaDelante {distancia <= 2} (caida ?c) (tamano ?t))
	(test(> ?t (* ?c 3)))
	=>
	(modify ?m (derecha 0) (izquierda 1))
)

;;REGLAS SOBRE CORRER
;; Si se acerca a un hoyo grande (tamaño de casilla mayor que 3), empieza a correr
;;(defrule cogerCarrerilla (declare (salience 15))
;;	?m <- (Mario {derecha == 1} {puedeSaltar == 1})
;;	(not(obstaculoDelante{distancia < 3}))
;;	(not(enemigoDelante{distanciaX < 3}))
;;	(not(caidaDelante{distancia < 4}))
;;	=>
;;	(modify ?m (corriendo 1))
;;)

;; Si no hay ningun obstaculo que requiera velocidad deja de correr
;;(defrule pararCarrerilla (declare (salience 10))
;;	?m <- (Mario {corriendo == 1})
;;	(obstaculoDelante{distancia < 3})
;;	=>
;;	(modify ?m (corriendo 0))
;;)

;; Si no hay ningun obstaculo que requiera velocidad deja de correr
;;(defrule pararCarrerilla2 (declare (salience 10))
;;	?m <- (Mario {corriendo == 1})
;;	(enemigoDelante{distanciaX < 3})
;;	=>
;;	(modify ?m (corriendo 0))
;;)

;; Si no hay ningun obstaculo que requiera velocidad deja de correr
;;(defrule pararCarrerilla3 (declare (salience 10))
;;	?m <- (Mario {corriendo == 1})
;;	(caidaDelante{distancia < 4})
;;	=>
;;	(modify ?m (corriendo 0))
;;)