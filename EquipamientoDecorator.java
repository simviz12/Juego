public abstract class EquipamientoDecorator implements Personaje {
    protected Personaje personaje;
    protected static final int MAX_STAT = 5;
    
    public EquipamientoDecorator(Personaje personaje) {
        this.personaje = personaje;
    }
    
    @Override
    public int getVida() {
        return Math.min(personaje.getVida(), MAX_STAT);
    }
    
    @Override
    public int getCritico() {
        return Math.min(personaje.getCritico(), MAX_STAT);
    }
    
    @Override
    public int getEscudo() {
        return Math.min(personaje.getEscudo(), MAX_STAT);
    }
    
    @Override
    public int getAtaque() {
        return Math.min(personaje.getAtaque(), MAX_STAT);
    }
    
    @Override
    public void setVida(int vida) {
        personaje.setVida(Math.min(vida, MAX_STAT));
    }
    
    @Override
    public void setCritico(int critico) {
        personaje.setCritico(Math.min(critico, MAX_STAT));
    }
    
    @Override
    public void setEscudo(int escudo) {
        personaje.setEscudo(Math.min(escudo, MAX_STAT));
    }
    
    @Override
    public void setAtaque(int ataque) {
        personaje.setAtaque(Math.min(ataque, MAX_STAT));
    }
    
    @Override
    public String getStats() {
        int vidaCapped = Math.min(getVida(), MAX_STAT);
        int criticoCapped = Math.min(getCritico(), MAX_STAT);
        int escudoCapped = Math.min(getEscudo(), MAX_STAT);
        int ataqueCapped = Math.min(getAtaque(), MAX_STAT);
        
        String baseStats = "Vida: " + vidaCapped + ", Crítico: " + criticoCapped + ", Escudo: " + escudoCapped + ", Ataque: " + ataqueCapped;
        
        if (getVida() > MAX_STAT || getCritico() > MAX_STAT || getEscudo() > MAX_STAT || getAtaque() > MAX_STAT) {
            baseStats += " (CAPPED at 5)";
        }
        
        return baseStats;
    }
}
