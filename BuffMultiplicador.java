public class BuffMultiplicador extends BuffDecorator {
    private final double multiplicador;
    
    public BuffMultiplicador(Personaje personaje, double multiplicador, int duration) {
        super(personaje, duration);
        this.multiplicador = multiplicador;
    }
    
    @Override
    public int getAtaque() {
        if (!isActive) {
            return personaje.getAtaque();
        }
        int ataqueMultiplicado = (int) (personaje.getAtaque() * multiplicador);
        return Math.min(ataqueMultiplicado, MAX_STAT);
    }
    
    @Override
    public String getStats() {
        String baseStats = personaje.getStats();
        if (isActive) {
            return baseStats + " + BuffMultiplicador(x" + multiplicador + " Ataque, " + duration + " turns)";
        }
        return baseStats;
    }
}
