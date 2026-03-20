public class Espada extends EquipamientoDecorator {
    private static final int CRITICO_BONUS = 1;
    private static final int MAX_SWORDS = 3;
    
    public Espada(Personaje personaje) {
        super(personaje);
        validateSwordLimit();
    }
    
    private void validateSwordLimit() {
        int swordCount = countSwords(personaje);
        if (swordCount >= MAX_SWORDS) {
            throw new TooManySwordsException("Cannot equip more than " + MAX_SWORDS + " swords");
        }
    }
    
    private int countSwords(Personaje p) {
        int count = 0;
        Personaje current = p;
        
        while (current instanceof EquipamientoDecorator) {
            if (current instanceof Espada) {
                count++;
            }
            current = ((EquipamientoDecorator) current).personaje;
        }
        
        return count;
    }
    
    @Override
    public int getCritico() {
        return Math.min(personaje.getCritico() + CRITICO_BONUS, MAX_STAT);
    }
    
    @Override
    public String getStats() {
        String baseStats = personaje.getStats();
        if (!baseStats.contains("(CAPPED")) {
            return baseStats + " + Espada(+1 Crítico)";
        }
        return baseStats;
    }
}
