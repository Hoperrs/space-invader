package SpaceInvader;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.swing.*;
import SpaceInvader.red.*;

public class InvasoresEspaciales extends JPanel implements ActionListener, KeyListener, ClienteJuego.EventosCliente {
    int tamanoCasilla = 40; //32
    int filas = 20; //16
    int columnas = 20;

    int anchoTablero = tamanoCasilla * columnas;
    int altoTablero = tamanoCasilla * filas;

    Image imagenNave;
    Image imagenAlien;
    Image imagenAlienCeleste;
    Image imagenAlienMorado;
    Image imagenAlienAmarillo;
    ArrayList<Image> imagenesAliens;

    // Cliente de red
    private ClienteJuego clienteJuego;
    private int idJugador = -1;
    private boolean conectado = false;
    private EstadoJuego estadoActual;
    private JLabel lblEstadoConexion;
    private Map<Integer, Color> coloresJugadores;

    class Bloque {
        int x;
        int y;
        int ancho;
        int alto;
        Image imagen;
        boolean vivo = true;
        boolean usado = false;
        int jugadorId = -1;

        Bloque(int x, int y, int ancho, int alto, Image imagen) {
            this.x = x;
            this.y = y;
            this.ancho = ancho;
            this.alto = alto;
            this.imagen = imagen;
        }
    }

    // nave
    int anchoNave = tamanoCasilla * 2;
    int altoNave = tamanoCasilla;
    int naveX = tamanoCasilla * columnas / 2 - tamanoCasilla;
    int naveY = tamanoCasilla * filas - tamanoCasilla * 2;
    int velocidadNave = tamanoCasilla;
    Bloque nave;

    // aliens
    ArrayList<Bloque> listaAliens;
    int anchoAlien = tamanoCasilla * 2;
    int altoAlien = tamanoCasilla;
    int alienX = tamanoCasilla;
    int alienY = tamanoCasilla;
    int filasAliens = 2;
    int columnasAliens = 3;
    int cantidadAliens = 0;
    int velocidadAlienX = 1;

    // bala nave
    ArrayList<Bloque> listaBalas;
    int anchoBala = tamanoCasilla / 8;
    int altoBala = tamanoCasilla / 2;
    int velocidadBalaY = -10;

    //balas enemigas
    ArrayList<Bloque> listaBalasEnemigas;
    int velocidadBalaEnemigaY = 3;
    int intervaloDisparoEnemigo = 30; // cada 10 frames
    int contadorDisparo = 0;

    //bloque protege
    ArrayList<Bloque> listaCoberturas;
    int vidaCobertura = 100; 
    HashMap<Bloque, Integer> mapaVidaCoberturas = new HashMap<>();

    Timer bucleJuego;
    boolean juegoTerminado = false;
    int puntaje = 0;

    InvasoresEspaciales() {
        setPreferredSize(new Dimension(anchoTablero, altoTablero));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);

        imagenNave = new ImageIcon(getClass().getResource("./ship.png")).getImage();
        imagenAlien = new ImageIcon(getClass().getResource("./alien.png")).getImage();
        imagenAlienCeleste = new ImageIcon(getClass().getResource("./alien-celeste.png")).getImage();
        imagenAlienMorado = new ImageIcon(getClass().getResource("./alien-morado.png")).getImage();
        imagenAlienAmarillo = new ImageIcon(getClass().getResource("./alien-amarillo.png")).getImage();

        imagenesAliens = new ArrayList<>();
        imagenesAliens.add(imagenAlien);
        imagenesAliens.add(imagenAlienCeleste);
        imagenesAliens.add(imagenAlienMorado);
        imagenesAliens.add(imagenAlienAmarillo);

        nave = new Bloque(naveX, naveY, anchoNave, altoNave, imagenNave);
        listaAliens = new ArrayList<>();
        listaBalas = new ArrayList<>();
        listaBalasEnemigas = new ArrayList<>();
        coloresJugadores = new HashMap<>();
        
        // Inicializar colores para jugadores
        coloresJugadores.put(0, Color.GREEN);
        coloresJugadores.put(1, Color.BLUE);
        coloresJugadores.put(2, Color.YELLOW);
        coloresJugadores.put(3, Color.MAGENTA);

        lblEstadoConexion = new JLabel("Conectando al servidor...");
        lblEstadoConexion.setForeground(Color.WHITE);
        this.add(lblEstadoConexion);

        // Inicializar cliente
        clienteJuego = new ClienteJuego(this);
        clienteJuego.conectar();

        bucleJuego = new Timer(1000 / 60, this);
        bucleJuego.start();

        listaCoberturas = new ArrayList<>();
        mapaVidaCoberturas = new HashMap<>();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        dibujar(g);
    }

    public void dibujar(Graphics g) {
        if (!conectado) {
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString("Conectando al servidor...", anchoTablero / 2 - 150, altoTablero / 2);
            return;
        }
        
        if (estadoActual == null) return;
        
        // Dibujar naves (jugadores)
        for (EstadoJuego.DatosBloque datosNave : estadoActual.getNaves()) {
            if (datosNave.vivo) {
                g.drawImage(imagenNave, datosNave.x, datosNave.y, datosNave.ancho, datosNave.alto, null);
                
                // Dibujar indicador de jugador
                g.setColor(coloresJugadores.getOrDefault(datosNave.jugadorId, Color.WHITE));
                g.drawString("J" + (datosNave.jugadorId + 1), datosNave.x + datosNave.ancho / 2 - 5, 
                          datosNave.y + datosNave.alto + 15);
            }
        }

        // Dibujar aliens
        for (EstadoJuego.DatosBloque datosAlien : estadoActual.getAliens()) {
            if (datosAlien.vivo) {
                // Usar imágenes diferentes según el tipo (para simplificar usamos el índice)
                int indiceImagen = datosAlien.x % imagenesAliens.size();
                g.drawImage(imagenesAliens.get(indiceImagen), datosAlien.x, datosAlien.y, 
                            datosAlien.ancho, datosAlien.alto, null);
            }
        }

        // Dibujar balas de jugadores
        g.setColor(Color.white);
        for (EstadoJuego.DatosBloque datosBala : estadoActual.getBalasJugadores()) {
            if (!datosBala.usado) {
                int jugadorId = datosBala.jugadorId;
                g.setColor(coloresJugadores.getOrDefault(jugadorId, Color.WHITE));
                g.fillRect(datosBala.x, datosBala.y, datosBala.ancho, datosBala.alto);
            }
        }

        // Dibujar balas enemigas
        g.setColor(Color.red);
        for (EstadoJuego.DatosBloque datosBala : estadoActual.getBalasEnemigos()) {
            if (!datosBala.usado) {
                g.fillRect(datosBala.x, datosBala.y, datosBala.ancho, datosBala.alto);
            }
        }

        // Dibujar coberturas
        for (int i = 0; i < estadoActual.getCoberturas().size(); i++) {
            EstadoJuego.DatosBloque datosCobertura = estadoActual.getCoberturas().get(i);
            Integer vida = estadoActual.getVidaCoberturas().get(i);
            
            if (vida != null && vida > 0) {
                if (vida > 3) g.setColor(Color.green);
                else if (vida > 1) g.setColor(Color.orange);
                else g.setColor(Color.red);
                
                g.fillRect(datosCobertura.x, datosCobertura.y, 
                           datosCobertura.ancho, datosCobertura.alto);
            }
        }
        
        // Dibujar puntajes
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        
        int yPuntaje = 30;
        for (Map.Entry<Integer, Integer> entry : estadoActual.getPuntajes().entrySet()) {
            int jugadorId = entry.getKey();
            int puntajeJugador = entry.getValue();
            g.setColor(coloresJugadores.getOrDefault(jugadorId, Color.WHITE));
            String textoJugador = "Jugador " + (jugadorId + 1) + ": " + puntajeJugador;
            
            // Destacar el jugador actual
            if (jugadorId == idJugador) {
                textoJugador = "TÚ: " + puntajeJugador;
                g.setFont(new Font("Arial", Font.BOLD, 20));
            } else {
                g.setFont(new Font("Arial", Font.PLAIN, 20));
            }
            
            g.drawString(textoJugador, 10, yPuntaje);
            yPuntaje += 25;
        }

        // Mensaje de juego terminado
        if (estadoActual.isJuegoTerminado()) {
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("JUEGO TERMINADO", anchoTablero / 2 - 180, altoTablero / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Presiona cualquier tecla para reiniciar", anchoTablero / 2 - 160, altoTablero / 2 + 40);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (estadoActual != null && estadoActual.isJuegoTerminado()) {
            clienteJuego.enviarAccion(Mensaje.AccionJugador.REINICIAR);
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            clienteJuego.enviarAccion(Mensaje.AccionJugador.MOVER_IZQUIERDA);
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            clienteJuego.enviarAccion(Mensaje.AccionJugador.MOVER_DERECHA);
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            clienteJuego.enviarAccion(Mensaje.AccionJugador.DISPARAR);
        }
    }
    
    // Implementación de la interfaz EventosCliente
    @Override
    public void onConexionEstablecida(int idJugador) {
        this.idJugador = idJugador;
        this.conectado = true;
        SwingUtilities.invokeLater(() -> {
            lblEstadoConexion.setText("Conectado como Jugador " + (idJugador + 1));
            repaint();
        });
        System.out.println("Conectado al servidor como Jugador " + (idJugador + 1));
    }

    @Override
    public void onEstadoJuegoActualizado(EstadoJuego estado) {
        // Usar SwingUtilities.invokeLater para asegurar que la actualización visual ocurra en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> {
            // Crear una copia profunda del estado para evitar problemas de concurrencia
            this.estadoActual = estado;
            // Forzar repaint para actualizar la visualización inmediatamente
            repaint();
            // Imprimir información de depuración sobre el estado recibido
            System.out.println("Estado actualizado renderizado: " + 
                             estado.getNaves().size() + " naves, " + 
                             estado.getAliens().size() + " aliens, " + 
                             estado.getBalasJugadores().size() + " balas jugadores");
        });
    }

    @Override
    public void onJuegoTerminado(EstadoJuego estado) {
        // Usar SwingUtilities.invokeLater para asegurar que la actualización visual ocurra en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> {
            this.estadoActual = estado;
            // Forzar repaint para actualizar la visualización inmediatamente
            repaint();
            System.out.println("Juego terminado recibido y renderizado");
        });
    }

    @Override
    public void onErrorConexion(Exception e) {
        System.err.println("Error de conexión: " + e.getMessage());
        SwingUtilities.invokeLater(() -> {
            lblEstadoConexion.setText("Error de conexión: " + e.getMessage());
        });
    }
}
