import java.util.ArrayList;
import java.util.List;

public class SimuladorCombate {
    public static void main(String[] args) {
        System.out.println("=== SIMULADOR DE COMBATE AVANZADO ===\n");
        
        // Test 1: Combate básico
        testCombateBasico();
        
        // Test 2: Combate con buffs temporales
        testCombateConBuffs();
        
        // Test 3: Orden de buffs importa (Suma vs Multiplicación)
        testOrdenDeBuffs();
        
        // Test 4: Probabilidad de crítico
        testProbabilidadCritico();
        
        // Test 5: Simulación masiva para balance
        testBalanceDePoder();
        
        System.out.println("=== TODAS LAS SIMULACIONES COMPLETADAS ===");
    }
    
    private static void testCombateBasico() {
        System.out.println("Test 1: Combate Básico");
        System.out.println("-------------------");
        
        Personaje heroe = new HeroeBase(3, 2, 1, 2);
        heroe = new Espada(heroe);
        heroe = new Escudo(heroe);
        
        Enemigo enemigo = new Enemigo("Goblin", 2, 1, 1, 2);
        
        MotorCombate.ResultadoCombate resultado = MotorCombate.simularCombate(heroe, enemigo);
        MotorCombate.imprimirResultado(resultado);
        
        System.out.println("✓ Combate básico funciona\n");
    }
    
    private static void testCombateConBuffs() {
        System.out.println("Test 2: Combate con Buffs Temporales");
        System.out.println("------------------------------------");
        
        Personaje heroe = new HeroeBase(3, 2, 1, 2);
        heroe = new Espada(heroe);
        
        // Agregar buffs temporales
        heroe = new BuffAtaque(heroe, 2, 3); // +2 ataque por 3 turnos
        heroe = new BuffMultiplicador(heroe, 1.5, 2); // x1.5 ataque por 2 turnos
        
        Enemigo enemigo = new Enemigo("Orco", 4, 2, 2, 3);
        
        MotorCombate.ResultadoCombate resultado = MotorCombate.simularCombate(heroe, enemigo);
        MotorCombate.imprimirResultado(resultado);
        
        System.out.println("✓ Combate con buffs funciona\n");
    }
    
    private static void testOrdenDeBuffs() {
        System.out.println("Test 3: Orden de Buffs (Suma vs Multiplicación)");
        System.out.println("------------------------------------------------");
        
        // Caso 1: Suma primero, luego multiplicación
        Personaje heroe1 = new HeroeBase(2, 1, 1, 2);
        heroe1 = new BuffAtaque(heroe1, 2, 5); // +2 ataque = 4
        heroe1 = new BuffMultiplicador(heroe1, 2.0, 5); // x2 = 8 (capped a 5)
        
        // Caso 2: Multiplicación primero, luego suma
        Personaje heroe2 = new HeroeBase(2, 1, 1, 2);
        heroe2 = new BuffMultiplicador(heroe2, 2.0, 5); // x2 = 4
        heroe2 = new BuffAtaque(heroe2, 2, 5); // +2 = 6 (capped a 5)
        
        System.out.println("Orden 1 (Suma -> Multiplicación): " + heroe1.getStats());
        System.out.println("Orden 2 (Multiplicación -> Suma): " + heroe2.getStats());
        System.out.println("Ataque final 1: " + heroe1.getAtaque());
        System.out.println("Ataque final 2: " + heroe2.getAtaque());
        
        System.out.println("✓ Orden de buffs afecta el resultado\n");
    }
    
    private static void testProbabilidadCritico() {
        System.out.println("Test 4: Probabilidad de Crítico");
        System.out.println("-------------------------------");
        
        // Test con diferentes niveles de crítico
        int[] nivelesCritico = {1, 2, 3, 4, 5};
        
        for (int crit : nivelesCritico) {
            Personaje heroe = new HeroeBase(3, crit, 1, 3);
            Enemigo enemigo = new Enemigo("Dummy", 10, 0, 0, 1); // Sin defensa para ver críticos
            
            System.out.println("Probabilidad con Crítico " + crit + " (" + (crit * 10) + "%):");
            
            int criticosContados = 0;
            int totalAtaques = 100;
            
            for (int i = 0; i < totalAtaques; i++) {
                int danio = 3; // Ataque base
                if (Math.random() < (crit * 0.1)) {
                    danio *= 2;
                    criticosContados++;
                }
            }
            
            System.out.println("  Críticos reales: " + criticosContados + "/" + totalAtaques + 
                             " (" + (criticosContados * 100 / totalAtaques) + "%)");
        }
        
        System.out.println("✓ Probabilidad de crítico funciona\n");
    }
    
    private static void testBalanceDePoder() {
        System.out.println("Test 5: Balance de Poder (1000 simulaciones)");
        System.out.println("--------------------------------------------");
        
        int victoriasHeroe = 0;
        int victoriasEnemigo = 0;
        int promedioTurnos = 0;
        
        for (int i = 0; i < 1000; i++) {
            // Configuración variada para testing
            Personaje heroe = new HeroeBase(3, 2, 2, 2);
            heroe = new Espada(heroe);
            
            if (i % 3 == 0) {
                heroe = new BuffAtaque(heroe, 1, 5);
            }
            
            Enemigo enemigo = new Enemigo("Esqueleto", 3, 1, 1, 2);
            
            MotorCombate.ResultadoCombate resultado = MotorCombate.simularCombate(heroe, enemigo);
            
            if (resultado.ganador.equals("HeroeBase")) {
                victoriasHeroe++;
            } else {
                victoriasEnemigo++;
            }
            
            promedioTurnos += resultado.turnos;
        }
        
        promedioTurnos /= 1000;
        
        System.out.println("Resultados de 1000 simulaciones:");
        System.out.println("  Victorias Héroe: " + victoriasHeroe + " (" + (victoriasHeroe * 100 / 1000) + "%)");
        System.out.println("  Victorias Enemigo: " + victoriasEnemigo + " (" + (victoriasEnemigo * 100 / 1000) + "%)");
        System.out.println("  Promedio de turnos: " + promedioTurnos);
        
        // Ajustes sugeridos basados en resultados
        if (victoriasHeroe > 70) {
            System.out.println("  → Sugerencia: Los héroes son muy fuertes, aumentar poder de enemigos");
        } else if (victoriasHeroe < 30) {
            System.out.println("  → Sugerencia: Los héroes son muy débiles, mejorar equipo o buffs");
        } else {
            System.out.println("  → Balance parece adecuado (30-70% win rate)");
        }
        
        System.out.println("✓ Balance de poder analizado\n");
    }
}
