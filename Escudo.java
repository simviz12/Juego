public class Escudo extends EquipamientoDecorator {
    private static final int ESCUDO_BONUS = 1;
    
    public Escudo(Personaje personaje) {
        super(personaje);
    }
    
    @Override
    public int getEscudo() {
        return Math.min(personaje.getEscudo() + ESCUDO_BONUS, MAX_STAT);
    }
    
    @Override
    public String getStats() {
        String baseStats = personaje.getStats();
        if (!baseStats.contains("(CAPPED")) {
            return baseStats + " + Escudo(+1 Escudo)";
        }
        return baseStats;
    }
}
