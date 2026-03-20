import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MotorCombate {
    private static final Random random = new Random();
    
    public static class ResultadoCombate {
        public String ganador;
        public List<String> log;
        public int turnos;
        public List<String> detailedCombatLog;
        
        public ResultadoCombate(String ganador, List<String> log, int turnos, List<String> detailedCombatLog) {
            this.ganador = ganador;
            this.log = log;
            this.turnos = turnos;
            this.detailedCombatLog = detailedCombatLog;
        }
    }
    
    public static ResultadoCombate simularCombate(Personaje atacante, Enemigo defensor) {
        List<String> log = new ArrayList<>();
        List<String> detailedLog = new ArrayList<>();
        int turno = 1;
        
        log.add("=== COMIENZA EL COMBATE ===");
        log.add(atacante.getStats());
        log.add(defensor.getStats());
        log.add("");
        
        while (atacante.getVida() > 0 && defensor.estaVivo()) {
            log.add("--- Turno " + turno + " ---");
            
            // Turno del atacante
            int danioAtacante = calcularDanio(atacante, defensor);
            boolean fueCritico = esCritico(atacante.getCritico()) && danioAtacante > atacante.getAtaque() - defensor.getEscudo();
            defensor.recibirDanio(danioAtacante);
            
            String detalleAtaque = String.format("Héroe atac%s con %s! Daño: %d", 
                fueCritico ? "ó con Crítico" : "", 
                fueCritico ? "Crítico" : "Ataque Normal", 
                danioAtacante);
            detailedLog.add(detalleAtaque);
            
            log.add(atacante.getClass().getSimpleName() + " ataca y causa " + danioAtacante + " de daño");
            log.add("Vida de " + defensor.getNombre() + ": " + defensor.getVida());
            
            if (!defensor.estaVivo()) {
                break;
            }
            
            // Turno del defensor
            int danioDefensor = calcularDanio(defensor, atacante);
            boolean fueCriticoEnemigo = esCritico(defensor.getCritico()) && danioDefensor > defensor.getAtaque() - atacante.getEscudo();
            int vidaActualAtacante = atacante.getVida();
            int nuevaVidaAtacante = Math.max(vidaActualAtacante - danioDefensor, 0);
            atacante.setVida(nuevaVidaAtacante);
            
            String detalleDefensa = String.format("%s contraatac%s con %s! Daño: %d", 
                defensor.getNombre(),
                fueCriticoEnemigo ? "ó con Crítico" : "",
                fueCriticoEnemigo ? "Crítico" : "Ataque Normal",
                danioDefensor);
            detailedLog.add(detalleDefensa);
            
            log.add(defensor.getNombre() + " contraataca y causa " + danioDefensor + " de daño");
            log.add("Vida de " + atacante.getClass().getSimpleName() + ": " + atacante.getVida());
            
            // Actualizar buffs temporales
            actualizarBuffs(atacante);
            
            turno++;
            log.add("");
        }
        
        String ganador = atacante.getVida() > 0 ? atacante.getClass().getSimpleName() : defensor.getNombre();
        log.add("=== GANADOR: " + ganador + " ===");
        log.add("Combate terminó en " + turno + " turnos");
        
        return new ResultadoCombate(ganador, log, turno, detailedLog);
    }
    
    private static int calcularDanio(Personaje atacante, Personaje defensor) {
        int ataque = atacante.getAtaque();
        int defensa = defensor.getEscudo();
        
        // Aplicar probabilidad de crítico
        if (esCritico(atacante.getCritico())) {
            ataque *= 2;
        }
        
        int danio = Math.max(ataque - defensa, 1); // Mínimo 1 de daño
        
        return danio;
    }
    
    private static boolean esCritico(int critico) {
        // Probabilidad de crítico basada en el stat (máximo 5 = 50% de probabilidad)
        double probabilidadCritico = critico * 0.1; // 5% por punto, máximo 50%
        return random.nextDouble() < probabilidadCritico;
    }
    
    private static void actualizarBuffs(Personaje personaje) {
        // Recorrer la cadena de decoradores para encontrar buffs
        Personaje actual = personaje;
        while (actual instanceof BuffDecorator) {
            BuffDecorator buff = (BuffDecorator) actual;
            buff.tick();
            if (!buff.isActive()) {
                // El buff expiró, pero no podemos removerlo de la cadena
                // Simplemente dejará de afectar las estadísticas
            }
            actual = ((BuffDecorator) actual).personaje;
        }
    }
    
    public static void imprimirResultado(ResultadoCombate resultado) {
        for (String linea : resultado.log) {
            System.out.println(linea);
        }
    }
}
