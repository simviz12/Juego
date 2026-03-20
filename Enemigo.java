public class Enemigo implements Personaje {
    private static final int MAX_STAT = 5;
    
    private int vida;
    private int critico;
    private int escudo;
    private int ataque;
    private String nombre;
    
    public Enemigo(String nombre, int vida, int critico, int escudo, int ataque) {
        this.nombre = nombre;
        this.vida = Math.min(vida, MAX_STAT);
        this.critico = Math.min(critico, MAX_STAT);
        this.escudo = Math.min(escudo, MAX_STAT);
        this.ataque = Math.min(ataque, MAX_STAT);
    }
    
    @Override
    public int getVida() {
        return vida;
    }
    
    @Override
    public int getCritico() {
        return critico;
    }
    
    @Override
    public int getEscudo() {
        return escudo;
    }
    
    @Override
    public int getAtaque() {
        return ataque;
    }
    
    @Override
    public void setVida(int vida) {
        this.vida = Math.min(Math.max(vida, 0), MAX_STAT);
    }
    
    @Override
    public void setCritico(int critico) {
        this.critico = Math.min(critico, MAX_STAT);
    }
    
    @Override
    public void setEscudo(int escudo) {
        this.escudo = Math.min(escudo, MAX_STAT);
    }
    
    @Override
    public void setAtaque(int ataque) {
        this.ataque = Math.min(ataque, MAX_STAT);
    }
    
    public String getNombre() {
        return nombre;
    }
    
    @Override
    public String getStats() {
        return nombre + " - Vida: " + vida + ", Crítico: " + critico + ", Escudo: " + escudo + ", Ataque: " + ataque;
    }
    
    public void recibirDanio(int danio) {
        int danioReal = Math.max(danio - escudo, 0);
        vida = Math.max(vida - danioReal, 0);
    }
    
    public boolean estaVivo() {
        return vida > 0;
    }
}
