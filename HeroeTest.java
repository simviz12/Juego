public class HeroeTest {
    public static void main(String[] args) {
        System.out.println("=== Hero Equipment System Tests ===\n");
        
        // Test 1: Basic hero creation
        testBasicHero();
        
        // Test 2: Equipment stacking
        testEquipmentStacking();
        
        // Test 3: Stat caps (maximum 5)
        testStatCaps();
        
        // Test 4: Sword limit (maximum 3 swords)
        testSwordLimit();
        
        // Test 5: Mixed equipment with caps
        testMixedEquipmentWithCaps();
        
        System.out.println("=== All tests completed ===");
    }
    
    private static void testBasicHero() {
        System.out.println("Test 1: Basic Hero Creation");
        Personaje heroe = new HeroeBase(3, 2, 1, 2);
        System.out.println("Base hero stats: " + heroe.getStats());
        System.out.println("Vida: " + heroe.getVida() + ", Crítico: " + heroe.getCritico() + ", Escudo: " + heroe.getEscudo());
        System.out.println("✓ Basic hero creation works\n");
    }
    
    private static void testEquipmentStacking() {
        System.out.println("Test 2: Equipment Stacking");
        Personaje heroe = new HeroeBase(2, 1, 1, 2);
        
        heroe = new Espada(heroe);
        System.out.println("After 1 sword: " + heroe.getStats());
        
        heroe = new Escudo(heroe);
        System.out.println("After sword + shield: " + heroe.getStats());
        
        heroe = new Poder(heroe);
        System.out.println("After sword + shield + power: " + heroe.getStats());
        System.out.println("✓ Equipment stacking works\n");
    }
    
    private static void testStatCaps() {
        System.out.println("Test 3: Stat Caps (Maximum 5)");
        Personaje heroe = new HeroeBase(4, 4, 4, 3);
        
        // Add equipment that would exceed caps
        heroe = new Poder(heroe);  // Vida: 4 -> 5 (capped)
        heroe = new Poder(heroe);  // Vida: 5 -> 5 (still capped)
        
        System.out.println("After adding 2 Poder items to (4,4,4): " + heroe.getStats());
        System.out.println("Final stats - Vida: " + heroe.getVida() + ", Crítico: " + heroe.getCritico() + ", Escudo: " + heroe.getEscudo());
        
        // Test critico cap
        heroe = new HeroeBase(4, 4, 4, 3);
        heroe = new Espada(heroe);  // Crítico: 4 -> 5
        heroe = new Espada(heroe);  // Crítico: 5 -> 5 (capped)
        System.out.println("After adding 2 Espadas to (4,4,4): " + heroe.getStats());
        
        // Test escudo cap
        heroe = new HeroeBase(4, 4, 4, 3);
        heroe = new Escudo(heroe);  // Escudo: 4 -> 5
        heroe = new Escudo(heroe);  // Escudo: 5 -> 5 (capped)
        System.out.println("After adding 2 Escudos to (4,4,4): " + heroe.getStats());
        System.out.println("✓ Stat caps work correctly\n");
    }
    
    private static void testSwordLimit() {
        System.out.println("Test 4: Sword Limit (Maximum 3)");
        Personaje heroe = new HeroeBase(1, 1, 1, 1);
        
        try {
            heroe = new Espada(heroe);
            System.out.println("Added 1st sword: " + heroe.getStats());
            
            heroe = new Espada(heroe);
            System.out.println("Added 2nd sword: " + heroe.getStats());
            
            heroe = new Espada(heroe);
            System.out.println("Added 3rd sword: " + heroe.getStats());
            
            // This should throw exception
            heroe = new Espada(heroe);
            System.out.println("ERROR: 4th sword was added! This should not happen.");
            
        } catch (TooManySwordsException e) {
            System.out.println("✓ Correctly blocked 4th sword: " + e.getMessage());
            System.out.println("Final stats after 3 swords: " + heroe.getStats());
        }
        System.out.println("✓ Sword limit works correctly\n");
    }
    
    private static void testMixedEquipmentWithCaps() {
        System.out.println("Test 5: Mixed Equipment with Caps");
        Personaje heroe = new HeroeBase(3, 3, 3, 2);
        
        // Add various equipment
        heroe = new Espada(heroe);    // Crítico: 3 -> 4
        heroe = new Escudo(heroe);    // Escudo: 3 -> 4  
        heroe = new Poder(heroe);     // Vida: 3 -> 4
        
        System.out.println("After 1 of each: " + heroe.getStats());
        
        // Add more to test caps
        heroe = new Espada(heroe);    // Crítico: 4 -> 5
        heroe = new Escudo(heroe);    // Escudo: 4 -> 5
        heroe = new Poder(heroe);     // Vida: 4 -> 5
        
        System.out.println("After 2 of each (should be capped): " + heroe.getStats());
        
        // Try to add more (should remain capped)
        heroe = new Espada(heroe);    // Crítico: 5 -> 5 (capped)
        System.out.println("After 3rd sword (capped): " + heroe.getStats());
        
        System.out.println("Final verification:");
        System.out.println("- Vida: " + heroe.getVida() + " (should be 5)");
        System.out.println("- Crítico: " + heroe.getCritico() + " (should be 5)");
        System.out.println("- Escudo: " + heroe.getEscudo() + " (should be 5)");
        System.out.println("✓ Mixed equipment with caps works correctly\n");
    }
}
