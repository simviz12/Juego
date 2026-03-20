public class GameState {
    private static GameState instance;
    private Personaje heroeGlobal;
    private Enemigo enemigoActual;
    private int heroX = 250; // Center position
    private int heroY = 250;
    private static final int MAX_POSITION = 500; // Game area size
    
    private GameState() {
        // Initialize with a default hero
        heroeGlobal = new HeroeBase(3, 2, 1, 2);
        enemigoActual = new Enemigo("Goblin", 2, 1, 1, 2);
    }
    
    public static synchronized GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }
    
    public Personaje getHeroe() {
        return heroeGlobal;
    }
    
    public void setHeroe(Personaje heroe) {
        this.heroeGlobal = heroe;
    }
    
    public Enemigo getEnemigoActual() {
        return enemigoActual;
    }
    
    public void setEnemigoActual(Enemigo enemigo) {
        this.enemigoActual = enemigo;
    }
    
    public int getHeroX() {
        return heroX;
    }
    
    public int getHeroY() {
        return heroY;
    }
    
    public void moveHero(int dx, int dy) {
        heroX = Math.max(0, Math.min(MAX_POSITION, heroX + dx));
        heroY = Math.max(0, Math.min(MAX_POSITION, heroY + dy));
    }
    
    public void setHeroPosition(int x, int y) {
        heroX = Math.max(0, Math.min(MAX_POSITION, x));
        heroY = Math.max(0, Math.min(MAX_POSITION, y));
    }
    
    public String getHeroeStatsJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"vida\":").append(heroeGlobal.getVida()).append(",");
        json.append("\"critico\":").append(heroeGlobal.getCritico()).append(",");
        json.append("\"escudo\":").append(heroeGlobal.getEscudo()).append(",");
        json.append("\"ataque\":").append(heroeGlobal.getAtaque()).append(",");
        json.append("\"posicion\":{\"x\":").append(heroX).append(",\"y\":").append(heroY).append("},");
        json.append("\"decoradores\":[");
        
        // Get decorator chain
        Personaje actual = heroeGlobal;
        boolean first = true;
        
        while (actual instanceof EquipamientoDecorator) {
            if (!first) json.append(",");
            json.append("\"").append(actual.getClass().getSimpleName()).append("\"");
            actual = ((EquipamientoDecorator) actual).personaje;
            first = false;
        }
        
        json.append("],");
        json.append("\"base\":\"").append(actual.getClass().getSimpleName()).append("\"");
        json.append("}");
        
        return json.toString();
    }
    
    public void resetHeroe() {
        heroeGlobal = new HeroeBase(3, 2, 1, 2);
        heroX = 250;
        heroY = 250;
    }
    
    public String equipar(String item) throws Exception {
        switch(item.toLowerCase()) {
            case "espada":
                heroeGlobal = new Espada(heroeGlobal);
                return "Espada equipada (+1 Crítico)";
            case "escudo":
                heroeGlobal = new Escudo(heroeGlobal);
                return "Escudo equipado (+1 Escudo)";
            case "poder":
                heroeGlobal = new Poder(heroeGlobal);
                return "Poder equipado (+1 Ataque)";
            case "buffataque":
                heroeGlobal = new BuffAtaque(heroeGlobal, 2, 3);
                return "Buff de Ataque activado (+2 Ataque, 3 turnos)";
            case "buffmultiplicador":
                heroeGlobal = new BuffMultiplicador(heroeGlobal, 2.0, 2);
                return "Buff Multiplicador activado (x2 Ataque, 2 turnos)";
            default:
                throw new Exception("Ítem desconocido: " + item);
        }
    }
}
