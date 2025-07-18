package SpaceInvader.red;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class EstadoJuego implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Estructura para representar bloques (naves, aliens, balas)
    public static class DatosBloque implements Serializable {
        private static final long serialVersionUID = 1L;
        public int x;
        public int y;
        public int ancho;
        public int alto;
        public int tipo; // 0: nave, 1: alien, 2: bala nave, 3: bala alien, 4: cobertura
        public boolean vivo;
        public boolean usado;
        public int jugadorId; // Para identificar a qué jugador pertenece
        
        public DatosBloque(int x, int y, int ancho, int alto, int tipo, 
                          boolean vivo, boolean usado, int jugadorId) {
            this.x = x;
            this.y = y;
            this.ancho = ancho;
            this.alto = alto;
            this.tipo = tipo;
            this.vivo = vivo;
            this.usado = usado;
            this.jugadorId = jugadorId;
        }
    }
    
    // Estado del juego
    private ArrayList<DatosBloque> naves;
    private ArrayList<DatosBloque> aliens;
    private ArrayList<DatosBloque> balasJugadores;
    private ArrayList<DatosBloque> balasEnemigos;
    private ArrayList<DatosBloque> coberturas;
    private HashMap<Integer, Integer> vidaCoberturas;
    private HashMap<Integer, Integer> puntajes;
    private boolean juegoTerminado;
    private int velocidadAlienX;
    private int cantidadAliens;
    
    public EstadoJuego() {
        naves = new ArrayList<>();
        aliens = new ArrayList<>();
        balasJugadores = new ArrayList<>();
        balasEnemigos = new ArrayList<>();
        coberturas = new ArrayList<>();
        vidaCoberturas = new HashMap<>();
        puntajes = new HashMap<>();
        juegoTerminado = false;
    }
    
    // Getters y setters
    public ArrayList<DatosBloque> getNaves() {
        return naves;
    }
    
    public ArrayList<DatosBloque> getAliens() {
        return aliens;
    }
    
    public ArrayList<DatosBloque> getBalasJugadores() {
        return balasJugadores;
    }
    
    public ArrayList<DatosBloque> getBalasEnemigos() {
        return balasEnemigos;
    }
    
    public ArrayList<DatosBloque> getCoberturas() {
        return coberturas;
    }
    
    public HashMap<Integer, Integer> getVidaCoberturas() {
        return vidaCoberturas;
    }
    
    public HashMap<Integer, Integer> getPuntajes() {
        return puntajes;
    }
    
    public boolean isJuegoTerminado() {
        return juegoTerminado;
    }
    
    public void setJuegoTerminado(boolean juegoTerminado) {
        this.juegoTerminado = juegoTerminado;
    }
    
    public int getVelocidadAlienX() {
        return velocidadAlienX;
    }
    
    public void setVelocidadAlienX(int velocidadAlienX) {
        this.velocidadAlienX = velocidadAlienX;
    }
    
    public int getCantidadAliens() {
        return cantidadAliens;
    }
    
    public void setCantidadAliens(int cantidadAliens) {
        this.cantidadAliens = cantidadAliens;
    }
    
    public void addNave(DatosBloque nave) {
        naves.add(nave);
    }
    
    public void addAlien(DatosBloque alien) {
        aliens.add(alien);
    }
    
    public void addBalaJugador(DatosBloque bala) {
        balasJugadores.add(bala);
    }
    
    public void addBalaEnemigo(DatosBloque bala) {
        balasEnemigos.add(bala);
    }
    
    public void addCobertura(DatosBloque cobertura, int vida) {
        coberturas.add(cobertura);
        vidaCoberturas.put(coberturas.size() - 1, vida);
    }
    
    public void setPuntaje(int jugadorId, int puntaje) {
        puntajes.put(jugadorId, puntaje);
    }
}