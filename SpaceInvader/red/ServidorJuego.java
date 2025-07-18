package SpaceInvader.red;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.Image;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServidorJuego {
    // Configuración del servidor
    private static final int PUERTO = 5000;
    private int maxJugadores = 4;
    private static final int TICK_RATE = 60; // actualizaciones por segundo
    
    // Configuración del juego
    private int tamanoCasilla = 40;
    private int filas = 20;
    private int columnas = 20;
    private int anchoTablero = tamanoCasilla * columnas;
    private int altoTablero = tamanoCasilla * filas;
    
    // Estado del juego
    private EstadoJuego estadoJuego;
    private int velocidadAlienX = 1;
    private int velocidadBalaY = -10;
    private int velocidadBalaEnemigaY = 3;
    private int intervaloDisparoEnemigo = 30;
    private int contadorDisparo = 0;
    private boolean juegoEnMarcha = false;
    private int filasAliens = 2;
    private int columnasAliens = 3;
    
    // Gestión de conexiones
    private ServerSocket servidorSocket;
    private ConcurrentHashMap<Integer, ClienteHandler> clientes;
    private AtomicInteger idJugadorSiguiente = new AtomicInteger(0);
    private boolean servidorActivo = true;
    
    // Constructor
    public ServidorJuego() {
        estadoJuego = new EstadoJuego();
        clientes = new ConcurrentHashMap<>();
        try {
            servidorSocket = new ServerSocket(PUERTO);
            System.out.println("Servidor iniciado en puerto " + PUERTO);
            // Thread para aceptar conexiones
            Thread hiloConexiones = new Thread(() -> aceptarConexiones());
            hiloConexiones.start();
            // Thread para la lógica del juego
            Thread hiloJuego = new Thread(() -> bucleJuego());
            hiloJuego.start();
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Método para aceptar conexiones entrantes
    private void aceptarConexiones() {
        while (servidorActivo) {
            try {
                Socket socketCliente = servidorSocket.accept();
                if (clientes.size() < maxJugadores) {
                    int idJugador = idJugadorSiguiente.getAndIncrement();
                    ClienteHandler clienteHandler = new ClienteHandler(socketCliente, idJugador);
                    clientes.put(idJugador, clienteHandler);
                    // Crear nave para el nuevo jugador
                    int naveX = tamanoCasilla * columnas / 2 - tamanoCasilla + (idJugador * tamanoCasilla * 2);
                    int naveY = tamanoCasilla * filas - tamanoCasilla * 2;
                    // Ajustar posición para que las naves no se salgan del tablero
                    if (naveX < 0) naveX = 0;
                    if (naveX > anchoTablero - tamanoCasilla * 2) naveX = anchoTablero - tamanoCasilla * 2;
                    EstadoJuego.DatosBloque datosNave = new EstadoJuego.DatosBloque(
                        naveX, naveY, tamanoCasilla * 2, tamanoCasilla, 
                        0, true, false, idJugador
                    );
                    synchronized (estadoJuego) {
                        estadoJuego.addNave(datosNave);
                        estadoJuego.setPuntaje(idJugador, 0);
                    }
                    Thread hiloCliente = new Thread(clienteHandler);
                    hiloCliente.start();
                    System.out.println("Nuevo jugador conectado: " + idJugador + " (" + clientes.size() + "/" + maxJugadores + ")");
                    // Iniciar el juego solo cuando todos los jugadores se hayan conectado
                    if (clientes.size() == maxJugadores) {
                        iniciarJuego();
                    }
                } else {
                    // Rechazar conexión si se alcanzó el máximo de jugadores
                    socketCliente.close();
                    System.out.println("Conexión rechazada: límite de jugadores alcanzado");
                }
            } catch (IOException e) {
                if (servidorActivo) {
                    System.err.println("Error al aceptar conexión: " + e.getMessage());
                }
            }
        }
    }
    
    // Método para iniciar el juego
    private void iniciarJuego() {
        synchronized (estadoJuego) {
            // Limpiar estado anterior
            estadoJuego.getAliens().clear();
            estadoJuego.getBalasJugadores().clear();
            estadoJuego.getBalasEnemigos().clear();
            
            // Crear aliens
            crearAliens();
            
            // Crear coberturas
            crearCoberturas();
            
            juegoEnMarcha = true;
        }
    }
    
    // Método para crear aliens
    private void crearAliens() {
        Random aleatorio = new Random();
        int anchoAlien = tamanoCasilla * 2;
        int altoAlien = tamanoCasilla;
        int alienX = tamanoCasilla;
        int alienY = tamanoCasilla;
        
        for (int c = 0; c < columnasAliens; c++) {
            for (int f = 0; f < filasAliens; f++) {
                int indiceImagen = aleatorio.nextInt(4); // Simula las diferentes imágenes
                EstadoJuego.DatosBloque datosAlien = new EstadoJuego.DatosBloque(
                    alienX + c * anchoAlien,
                    alienY + f * altoAlien,
                    anchoAlien,
                    altoAlien,
                    1, // Tipo alien
                    true,
                    false,
                    -1 // ID -1 para los aliens
                );
                estadoJuego.addAlien(datosAlien);
            }
        }
        
        estadoJuego.setCantidadAliens(columnasAliens * filasAliens);
        estadoJuego.setVelocidadAlienX(velocidadAlienX);
    }
    
    // Método para crear coberturas
    private void crearCoberturas() {
        int cantidadCoberturas = 4;
        int anchoCobertura = tamanoCasilla * 2;
        int altoCobertura = tamanoCasilla;
        int espacioEntre = (anchoTablero - cantidadCoberturas * anchoCobertura) / (cantidadCoberturas + 1);
        int yCobertura = tamanoCasilla * filas - tamanoCasilla * 4;
        
        for (int i = 0; i < cantidadCoberturas; i++) {
            int xCobertura = espacioEntre + i * (anchoCobertura + espacioEntre);
            EstadoJuego.DatosBloque datosCobertura = new EstadoJuego.DatosBloque(
                xCobertura, yCobertura, anchoCobertura, altoCobertura,
                4, // Tipo cobertura
                true, 
                false,
                -1 // ID -1 para coberturas
            );
            estadoJuego.addCobertura(datosCobertura, 5); // Vida inicial 5
        }
    }
    
    // Bucle principal del juego
    private void bucleJuego() {
        long tiempoAnterior = System.currentTimeMillis();
        long intervaloTick = 1000 / TICK_RATE;
        
        while (servidorActivo) {
            long tiempoActual = System.currentTimeMillis();
            long tiempoTranscurrido = tiempoActual - tiempoAnterior;
            
            if (tiempoTranscurrido >= intervaloTick) {
                tiempoAnterior = tiempoActual;
                
                if (juegoEnMarcha) {
                    actualizarEstadoJuego();
                    enviarEstadoATodos();
                }
            }
            
            try {
                Thread.sleep(1); // Reducir uso de CPU
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Método para actualizar el estado del juego
    private void actualizarEstadoJuego() {
        synchronized (estadoJuego) {
            // Actualizar aliens
            moverAliens();
            
            // Actualizar balas de jugadores
            moverBalasJugadores();
            
            // Disparos de aliens
            gestionarDisparosEnemigos();
            
            // Actualizar balas enemigas
            moverBalasEnemigas();
            
            // Comprobar fin del nivel
            comprobarFinNivel();
        }
    }
    
    // Método para mover los aliens
    private void moverAliens() {
        boolean cambiarDireccion = false;
        int velocidadX = estadoJuego.getVelocidadAlienX();
        
        for (EstadoJuego.DatosBloque alien : estadoJuego.getAliens()) {
            if (alien.vivo) {
                alien.x += velocidadX;
                
                if (alien.x + alien.ancho >= anchoTablero || alien.x <= 0) {
                    cambiarDireccion = true;
                }
                
                // Verificar si algún alien llegó a la altura de las naves
                for (EstadoJuego.DatosBloque nave : estadoJuego.getNaves()) {
                    if (alien.y >= nave.y) {
                        estadoJuego.setJuegoTerminado(true);
                        break;
                    }
                }
            }
        }
        
        if (cambiarDireccion) {
            velocidadX *= -1;
            estadoJuego.setVelocidadAlienX(velocidadX);
            
            // Mover aliens hacia abajo
            for (EstadoJuego.DatosBloque alien : estadoJuego.getAliens()) {
                if (alien.vivo) {
                    alien.y += altoTablero / 20; // 5% de la altura total
                }
            }
        }
    }
    
    // Método para mover las balas de los jugadores
    private void moverBalasJugadores() {
        Iterator<EstadoJuego.DatosBloque> iteradorBalas = estadoJuego.getBalasJugadores().iterator();
        
        while (iteradorBalas.hasNext()) {
            EstadoJuego.DatosBloque bala = iteradorBalas.next();
            
            if (!bala.usado) {
                bala.y += velocidadBalaY;
                
                // Verificar colisiones con aliens
                for (EstadoJuego.DatosBloque alien : estadoJuego.getAliens()) {
                    if (!bala.usado && alien.vivo && detectarColision(bala, alien)) {
                        bala.usado = true;
                        alien.vivo = false;
                        estadoJuego.setCantidadAliens(estadoJuego.getCantidadAliens() - 1);
                        
                        // Aumentar puntaje del jugador
                        int jugadorId = bala.jugadorId;
                        int puntajeActual = estadoJuego.getPuntajes().getOrDefault(jugadorId, 0);
                        estadoJuego.setPuntaje(jugadorId, puntajeActual + 100);
                    }
                }
                
                // Verificar colisiones con coberturas
                for (int i = 0; i < estadoJuego.getCoberturas().size(); i++) {
                    EstadoJuego.DatosBloque cobertura = estadoJuego.getCoberturas().get(i);
                    Integer vida = estadoJuego.getVidaCoberturas().get(i);
                    
                    if (!bala.usado && vida > 0 && detectarColision(bala, cobertura)) {
                        bala.usado = true;
                        estadoJuego.getVidaCoberturas().put(i, vida - 1);
                    }
                }
                
                // Eliminar balas que salen de la pantalla
                if (bala.y < 0) {
                    bala.usado = true;
                }
            }
        }
        
        // Eliminar balas usadas
        estadoJuego.getBalasJugadores().removeIf(bala -> bala.usado);
    }
    
    // Método para gestionar los disparos de los enemigos
    private void gestionarDisparosEnemigos() {
        contadorDisparo++;
        
        if (contadorDisparo >= intervaloDisparoEnemigo) {
            contadorDisparo = 0;
            
            ArrayList<EstadoJuego.DatosBloque> aliensVivos = new ArrayList<>();
            for (EstadoJuego.DatosBloque alien : estadoJuego.getAliens()) {
                if (alien.vivo) aliensVivos.add(alien);
            }
            
            if (!aliensVivos.isEmpty()) {
                Random rand = new Random();
                EstadoJuego.DatosBloque alienDisparador = aliensVivos.get(rand.nextInt(aliensVivos.size()));
                
                EstadoJuego.DatosBloque balaEnemiga = new EstadoJuego.DatosBloque(
                    alienDisparador.x + alienDisparador.ancho / 2,
                    alienDisparador.y + alienDisparador.alto,
                    tamanoCasilla / 8,
                    tamanoCasilla / 2,
                    3, // Tipo bala enemiga
                    true,
                    false,
                    -1  // ID -1 para balas enemigas
                );
                
                estadoJuego.addBalaEnemigo(balaEnemiga);
            }
        }
    }
    
    // Método para mover las balas enemigas
    private void moverBalasEnemigas() {
        Iterator<EstadoJuego.DatosBloque> iteradorBalas = estadoJuego.getBalasEnemigos().iterator();
        
        while (iteradorBalas.hasNext()) {
            EstadoJuego.DatosBloque bala = iteradorBalas.next();
            
            if (!bala.usado) {
                bala.y += velocidadBalaEnemigaY;
                
                // Verificar colisiones con naves
                for (EstadoJuego.DatosBloque nave : estadoJuego.getNaves()) {
                    if (!bala.usado && nave.vivo && detectarColision(bala, nave)) {
                        bala.usado = true;
                        nave.vivo = false;
                        
                        // Verificar si todos los jugadores perdieron
                        boolean todosEliminados = true;
                        for (EstadoJuego.DatosBloque n : estadoJuego.getNaves()) {
                            if (n.vivo) {
                                todosEliminados = false;
                                break;
                            }
                        }
                        
                        if (todosEliminados) {
                            estadoJuego.setJuegoTerminado(true);
                        }
                    }
                }
                
                // Verificar colisiones con coberturas
                for (int i = 0; i < estadoJuego.getCoberturas().size(); i++) {
                    EstadoJuego.DatosBloque cobertura = estadoJuego.getCoberturas().get(i);
                    Integer vida = estadoJuego.getVidaCoberturas().get(i);
                    
                    if (!bala.usado && vida > 0 && detectarColision(bala, cobertura)) {
                        bala.usado = true;
                        estadoJuego.getVidaCoberturas().put(i, vida - 1);
                    }
                }
                
                // Eliminar balas que salen de la pantalla
                if (bala.y > altoTablero) {
                    bala.usado = true;
                }
            }
        }
        
        // Eliminar balas usadas
        estadoJuego.getBalasEnemigos().removeIf(bala -> bala.usado);
    }
    
    // Método para comprobar si el nivel ha terminado
    private void comprobarFinNivel() {
        if (estadoJuego.getCantidadAliens() == 0) {
            // Sumar puntos extra al finalizar nivel
            for (Integer jugadorId : estadoJuego.getPuntajes().keySet()) {
                int puntajeActual = estadoJuego.getPuntajes().get(jugadorId);
                estadoJuego.setPuntaje(jugadorId, puntajeActual + (columnasAliens * filasAliens * 100));
            }
            
            // Aumentar dificultad
            columnasAliens = Math.min(columnasAliens + 1, columnas / 2 - 2);
            filasAliens = Math.min(filasAliens + 1, filas - 6);
            
            // Reiniciar nivel
            iniciarJuego();
        }
    }
    
    // Método para detectar colisiones entre dos bloques
    private boolean detectarColision(EstadoJuego.DatosBloque a, EstadoJuego.DatosBloque b) {
        return a.x < b.x + b.ancho &&
               a.x + a.ancho > b.x &&
               a.y < b.y + b.alto &&
               a.y + a.alto > b.y;
    }
    
    // Método para enviar el estado actual a todos los clientes
    private void enviarEstadoATodos() {
        // Crear un nuevo mensaje para cada cliente para evitar problemas de serialización
        for (ClienteHandler cliente : clientes.values()) {
            // Crear un mensaje nuevo para cada cliente
            Mensaje mensajeEstado = new Mensaje(Mensaje.TipoMensaje.ESTADO_JUEGO, estadoJuego);
            cliente.enviarMensaje(mensajeEstado);
        }
        
        if (estadoJuego.isJuegoTerminado()) {
            for (ClienteHandler cliente : clientes.values()) {
                // Crear un mensaje nuevo para cada cliente
                Mensaje mensajeFinalJuego = new Mensaje(Mensaje.TipoMensaje.FINAL_JUEGO, estadoJuego);
                cliente.enviarMensaje(mensajeFinalJuego);
            }
        }
    }
    
    // Método para procesar las acciones de un jugador
    private void procesarAccionJugador(int idJugador, Mensaje.AccionJugador accion) {
        synchronized (estadoJuego) {
            // Buscar la nave del jugador
            EstadoJuego.DatosBloque naveJugador = null;
            for (EstadoJuego.DatosBloque nave : estadoJuego.getNaves()) {
                if (nave.jugadorId == idJugador && nave.vivo) {
                    naveJugador = nave;
                    break;
                }
            }
            
            if (naveJugador != null) {
                switch (accion) {
                    case MOVER_IZQUIERDA:
                        if (naveJugador.x - tamanoCasilla >= 0) {
                            naveJugador.x -= tamanoCasilla;
                        }
                        break;
                    case MOVER_DERECHA:
                        if (naveJugador.x + tamanoCasilla + naveJugador.ancho <= anchoTablero) {
                            naveJugador.x += tamanoCasilla;
                        }
                        break;
                    case DISPARAR:
                        EstadoJuego.DatosBloque bala = new EstadoJuego.DatosBloque(
                            naveJugador.x + naveJugador.ancho * 15 / 32,
                            naveJugador.y,
                            tamanoCasilla / 8,
                            tamanoCasilla / 2,
                            2, // Tipo bala jugador
                            true,
                            false,
                            idJugador
                        );
                        estadoJuego.addBalaJugador(bala);
                        break;
                    case REINICIAR:
                        if (estadoJuego.isJuegoTerminado()) {
                            // Reiniciar el juego si todos los jugadores están de acuerdo
                            // Para simplificar, cualquier jugador puede reiniciar
                            reiniciarJuego();
                        }
                        break;
                }
            }
        }
    }
    
    // Método para reiniciar el juego
    private void reiniciarJuego() {
        synchronized (estadoJuego) {
            // Restablecer puntajes
            for (Integer jugadorId : estadoJuego.getPuntajes().keySet()) {
                estadoJuego.setPuntaje(jugadorId, 0);
            }
            
            // Restablecer naves
            for (EstadoJuego.DatosBloque nave : estadoJuego.getNaves()) {
                nave.vivo = true;
                nave.x = tamanoCasilla * columnas / 2 - tamanoCasilla + (nave.jugadorId * tamanoCasilla * 2);
                if (nave.x < 0) nave.x = 0;
                if (nave.x > anchoTablero - nave.ancho) nave.x = anchoTablero - nave.ancho;
            }
            
            // Restablecer nivel
            columnasAliens = 3;
            filasAliens = 2;
            velocidadAlienX = 1;
            
            estadoJuego.setJuegoTerminado(false);
            
            // Reiniciar juego
            iniciarJuego();
        }
    }
    
    // Método para eliminar un jugador
    private void eliminarJugador(int idJugador) {
        synchronized (estadoJuego) {
            // Eliminar la nave del jugador
            for (EstadoJuego.DatosBloque nave : estadoJuego.getNaves()) {
                if (nave.jugadorId == idJugador) {
                    nave.vivo = false;
                    break;
                }
            }
            
            // Eliminar el cliente de la lista
            clientes.remove(idJugador);
            
            System.out.println("Jugador " + idJugador + " desconectado");
            
            // Si no quedan jugadores, reiniciar el juego
            if (clientes.isEmpty()) {
                juegoEnMarcha = false;
            }
        }
    }
    
    // Método principal para iniciar el servidor
    public static void main(String[] args) {
        ServidorJuego servidor;
        if (args != null && args.length > 0) {
            try {
                int cantidad = Integer.parseInt(args[0]);
                if (cantidad >= 2 && cantidad <= 4) {
                    servidor = new ServidorJuego();
                    servidor.maxJugadores = cantidad;
                } else {
                    servidor = new ServidorJuego();
                }
            } catch (NumberFormatException e) {
                servidor = new ServidorJuego();
            }
        } else {
            servidor = new ServidorJuego();
        }
    }
    
    // Clase interna para manejar las conexiones con clientes
    private class ClienteHandler implements Runnable {
        private Socket socket;
        private int idJugador;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private boolean conectado = true;
        
        public ClienteHandler(Socket socket, int idJugador) {
            this.socket = socket;
            this.idJugador = idJugador;
            
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                // Enviar ID de jugador al cliente
                Mensaje mensajeRegistro = new Mensaje(Mensaje.TipoMensaje.REGISTRO);
                mensajeRegistro.setIdJugador(idJugador);
                enviarMensaje(mensajeRegistro);
                
            } catch (IOException e) {
                System.err.println("Error al configurar streams para cliente " + idJugador + ": " + e.getMessage());
                conectado = false;
            }
        }
        
        @Override
        public void run() {
            try {
                while (conectado) {
                    Object obj = in.readObject();
                    
                    if (obj instanceof Mensaje) {
                        Mensaje mensaje = (Mensaje) obj;
                        
                        if (mensaje.getTipo() == Mensaje.TipoMensaje.ACCION_JUGADOR) {
                            procesarAccionJugador(idJugador, mensaje.getAccion());
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error en comunicación con cliente " + idJugador + ": " + e.getMessage());
            } finally {
                desconectar();
            }
        }
        
        public void enviarMensaje(Mensaje mensaje) {
            try {
                // Reset del ObjectOutputStream para evitar problemas de caché con objetos referenciados
                out.reset();
                out.writeObject(mensaje);
                out.flush();
                System.out.println("Mensaje enviado a cliente " + idJugador + ": " + mensaje.getTipo());
            } catch (IOException e) {
                System.err.println("Error al enviar mensaje a cliente " + idJugador + ": " + e.getMessage());
                desconectar();
            }
        }
        
        private void desconectar() {
            if (conectado) {
                conectado = false;
                eliminarJugador(idJugador);
                
                try {
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    System.err.println("Error al cerrar socket: " + e.getMessage());
                }
            }
        }
    }
}