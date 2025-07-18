package SpaceInvader.red;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClienteJuego {
    private static String SERVIDOR_IP = "localhost";
    private static final int SERVIDOR_PUERTO = 5000;
    
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private int idJugador = -1;
    private boolean conectado = false;
    private Thread hiloRecepcion;
    
    // Cola para mensajes recibidos del servidor
    private BlockingQueue<Mensaje> colaMensajes = new LinkedBlockingQueue<>();
    
    // Interfaz para manejar eventos del cliente
    public interface EventosCliente {
        void onConexionEstablecida(int idJugador);
        void onEstadoJuegoActualizado(EstadoJuego estado);
        void onJuegoTerminado(EstadoJuego estado);
        void onErrorConexion(Exception e);
    }
    
    private EventosCliente manejadorEventos;
    
    public ClienteJuego(EventosCliente manejadorEventos) {
        this(manejadorEventos, null);
    }

    public ClienteJuego(EventosCliente manejadorEventos, String ipServidor) {
        this.manejadorEventos = manejadorEventos;
        if (ipServidor != null && !ipServidor.isEmpty()) {
            SERVIDOR_IP = ipServidor;
        }
    }
    
    // Método para conectar al servidor
    public void conectar() {
        try {
            socket = new Socket(SERVIDOR_IP, SERVIDOR_PUERTO);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            conectado = true;
            
            // Iniciar hilo para recibir mensajes
            hiloRecepcion = new Thread(this::procesarMensajesEntrantes);
            hiloRecepcion.start();
            
        } catch (IOException e) {
            if (manejadorEventos != null) {
                manejadorEventos.onErrorConexion(e);
            }
        }
    }
    
    // Método para enviar acción del jugador al servidor
    public void enviarAccion(Mensaje.AccionJugador accion) {
        if (conectado && idJugador != -1) {
            try {
                Mensaje mensaje = new Mensaje(Mensaje.TipoMensaje.ACCION_JUGADOR, idJugador, accion);
                out.writeObject(mensaje);
                out.flush();
            } catch (IOException e) {
                if (manejadorEventos != null) {
                    manejadorEventos.onErrorConexion(e);
                }
                desconectar();
            }
        }
    }
    
    // Método para obtener el siguiente mensaje recibido (no bloqueante)
    public Mensaje obtenerSiguienteMensaje() {
        return colaMensajes.poll();
    }
    
    // Hilo para procesar mensajes entrantes
    private void procesarMensajesEntrantes() {
        try {
            while (conectado) {
                try {
                    Object obj = in.readObject();
                    
                    if (obj instanceof Mensaje) {
                        Mensaje mensaje = (Mensaje) obj;
                        System.out.println("Mensaje recibido del servidor: " + mensaje.getTipo());
                        
                        switch (mensaje.getTipo()) {
                            case REGISTRO:
                                this.idJugador = mensaje.getIdJugador();
                                if (manejadorEventos != null) {
                                    manejadorEventos.onConexionEstablecida(idJugador);
                                }
                                break;
                            case ESTADO_JUEGO:
                                EstadoJuego estado = mensaje.getEstadoJuego();
                                if (estado != null && manejadorEventos != null) {
                                    manejadorEventos.onEstadoJuegoActualizado(estado);
                                }
                                colaMensajes.add(mensaje);
                                break;
                            case FINAL_JUEGO:
                                EstadoJuego estadoFinal = mensaje.getEstadoJuego();
                                if (estadoFinal != null && manejadorEventos != null) {
                                    manejadorEventos.onJuegoTerminado(estadoFinal);
                                }
                                colaMensajes.add(mensaje);
                                break;
                        }
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Error al deserializar objeto: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            if (conectado) {
                System.err.println("Error en comunicación con el servidor: " + e.getMessage());
                e.printStackTrace();
                if (manejadorEventos != null) {
                    manejadorEventos.onErrorConexion(e);
                }
                desconectar();
            }
        }
    }
    
    // Método para desconectar del servidor
    public void desconectar() {
        if (conectado) {
            conectado = false;
            try {
                if (socket != null) socket.close();
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Getter para el ID del jugador
    public int getIdJugador() {
        return idJugador;
    }
    
    // Verificar si está conectado
    public boolean estaConectado() {
        return conectado;
    }
}