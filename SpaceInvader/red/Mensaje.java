package SpaceInvader.red;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum TipoMensaje {
        REGISTRO,
        ACCION_JUGADOR,
        ESTADO_JUEGO,
        FINAL_JUEGO
    }
    
    public enum AccionJugador {
        MOVER_IZQUIERDA,
        MOVER_DERECHA,
        DISPARAR,
        REINICIAR
    }
    
    private TipoMensaje tipo;
    private Integer idJugador;
    private AccionJugador accion;
    private EstadoJuego estadoJuego;
    
    // Constructor para registro
    public Mensaje(TipoMensaje tipo) {
        this.tipo = tipo;
    }
    
    // Constructor para acción de jugador
    public Mensaje(TipoMensaje tipo, Integer idJugador, AccionJugador accion) {
        this.tipo = tipo;
        this.idJugador = idJugador;
        this.accion = accion;
    }
    
    // Constructor para enviar estado de juego
    public Mensaje(TipoMensaje tipo, EstadoJuego estadoJuego) {
        this.tipo = tipo;
        this.estadoJuego = estadoJuego;
    }
    
    // Getters y setters
    public TipoMensaje getTipo() {
        return tipo;
    }
    
    public Integer getIdJugador() {
        return idJugador;
    }
    
    public AccionJugador getAccion() {
        return accion;
    }
    
    public EstadoJuego getEstadoJuego() {
        return estadoJuego;
    }
    
    public void setIdJugador(Integer idJugador) {
        this.idJugador = idJugador;
    }
}