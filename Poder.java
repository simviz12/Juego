public class Poder extends EquipamientoDecorator {
    private static final int VIDA_BONUS = 1;
    
    public Poder(Personaje personaje) {
        super(personaje);
    }
    
    @Override
    public int getVida() {
        return Math.min(personaje.getVida() + VIDA_BONUS, MAX_STAT);
    }
    
    @Override
    public String getStats() {
        String baseStats = personaje.getStats();
        if (!baseStats.contains("(CAPPED")) {
            return baseStats + " + Poder(+1 Vida)";
        }
        return baseStats;
    }
}
