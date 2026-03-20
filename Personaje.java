public interface Personaje {
    int getVida();
    int getCritico();
    int getEscudo();
    int getAtaque();
    
    void setVida(int vida);
    void setCritico(int critico);
    void setEscudo(int escudo);
    void setAtaque(int ataque);
    
    String getStats();
}
