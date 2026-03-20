public class BuffAtaque extends BuffDecorator {
    private final int ataqueBonus;
    
    public BuffAtaque(Personaje personaje, int ataqueBonus, int duration) {
        super(personaje, duration);
        this.ataqueBonus = ataqueBonus;
    }
    
    @Override
    public int getAtaque() {
        if (!isActive) {
            return personaje.getAtaque();
        }
        return Math.min(personaje.getAtaque() + ataqueBonus, MAX_STAT);
    }
    
    @Override
    public String getStats() {
        String baseStats = personaje.getStats();
        if (isActive) {
            return baseStats + " + BuffAtaque(+" + ataqueBonus + " Ataque, " + duration + " turns)";
        }
        return baseStats;
    }
}
