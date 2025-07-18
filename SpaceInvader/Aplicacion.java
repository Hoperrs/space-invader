package SpaceInvader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Aplicacion {
    private static JFrame ventana;
    
    public static void main(String[] args) {
        // Preguntar al usuario si quiere iniciar como cliente o como servidor
        String[] opciones = {"Cliente", "Servidor"};
        int seleccion = JOptionPane.showOptionDialog(null, 
            "¿Qué deseas iniciar?", 
            "Space Invader Multijugador", 
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.QUESTION_MESSAGE, 
            null, opciones, opciones[0]);
        
        if (seleccion == 0) {
            // Iniciar como cliente
            iniciarCliente();
        } else if (seleccion == 1) {
            // Iniciar como servidor
            iniciarServidor();
        }
    }
    
    private static void iniciarCliente() {
        // Variables de la ventana
        int tamanoCasilla = 40;
        int filas = 20;
        int columnas = 20;
        int anchoTablero = tamanoCasilla * columnas;
        int altoTablero = tamanoCasilla * filas;

        ventana = new JFrame("Space Invader - Cliente");
        ventana.setSize(anchoTablero, altoTablero);
        ventana.setLocationRelativeTo(null);
        ventana.setResizable(false);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        InvasoresEspaciales juego = new InvasoresEspaciales();
        ventana.add(juego);
        ventana.pack();
        juego.requestFocus();
        ventana.setVisible(true);
    }
    
    private static void iniciarServidor() {
        // Pedir cantidad de jugadores
        int cantidadJugadoresTmp = 2;
        String input = JOptionPane.showInputDialog(null, "¿Cuántos jugadores deseas permitir? (2-4)", "Cantidad de jugadores", JOptionPane.QUESTION_MESSAGE);
        if (input != null) {
            try {
                cantidadJugadoresTmp = Integer.parseInt(input);
                if (cantidadJugadoresTmp < 2 || cantidadJugadoresTmp > 4) cantidadJugadoresTmp = 2;
            } catch (NumberFormatException ex) {
                cantidadJugadoresTmp = 2;
            }
        }
        final int cantidadJugadores = cantidadJugadoresTmp;

        // Crear una ventana para mostrar información del servidor
        JFrame ventanaServidor = new JFrame("Space Invader - Servidor");
        ventanaServidor.setSize(500, 400);
        ventanaServidor.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventanaServidor.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        JTextArea areaLog = new JTextArea();
        areaLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(areaLog);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JButton btnIniciar = new JButton("Iniciar Servidor");
        JButton btnDetener = new JButton("Detener Servidor");
        btnDetener.setEnabled(false);
        
        JPanel panelBotones = new JPanel();
        panelBotones.add(btnIniciar);
        panelBotones.add(btnDetener);
        panel.add(panelBotones, BorderLayout.SOUTH);
        
        ventanaServidor.add(panel);
        ventanaServidor.setVisible(true);
        
        // Redirigir la salida estándar y de error al área de texto
        System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
            @Override
            public void write(int b) {
                areaLog.append(String.valueOf((char) b));
                areaLog.setCaretPosition(areaLog.getDocument().getLength());
            }
        }));
        
        System.setErr(new java.io.PrintStream(new java.io.OutputStream() {
            @Override
            public void write(int b) {
                areaLog.append(String.valueOf((char) b));
                areaLog.setCaretPosition(areaLog.getDocument().getLength());
            }
        }));
        
        btnIniciar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    try {
                        // Instanciar el servidor pasando la cantidad de jugadores como argumento
                        SpaceInvader.red.ServidorJuego.main(new String[]{String.valueOf(cantidadJugadores)});
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
                btnIniciar.setEnabled(false);
                btnDetener.setEnabled(true);
                System.out.println("Servidor iniciado. Esperando conexiones...");
            }
        });
        
        btnDetener.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Esto no detiene realmente el servidor, pero es una indicación visual
                // Para detener realmente el servidor, necesitaríamos implementar un mecanismo de cierre en ServidorJuego
                btnIniciar.setEnabled(true);
                btnDetener.setEnabled(false);
                System.out.println("Nota: Para detener completamente el servidor, cierre esta ventana.");
                System.out.println("Los clientes conectados pueden experimentar errores de conexión.");
            }
        });
    }
}
