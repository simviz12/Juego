public class HeroeBase implements Personaje {
    private int vida;
    private int critico;
    private int escudo;
    private int ataque;
    
    public HeroeBase(int vida, int critico, int escudo, int ataque) {
        this.vida = vida;
        this.critico = critico;
        this.escudo = escudo;
        this.ataque = ataque;
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
        this.vida = vida;
    }
    
    @Override
    public void setCritico(int critico) {
        this.critico = critico;
    }
    
    @Override
    public void setEscudo(int escudo) {
        this.escudo = escudo;
    }
    
    @Override
    public void setAtaque(int ataque) {
        this.ataque = ataque;
    }
    
    @Override
    public String getStats() {
        return "Vida: " + vida + ", Crítico: " + critico + ", Escudo: " + escudo + ", Ataque: " + ataque;
    }
}
