import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class JuegoServidor2D {
    private static final int PORT = 8080;
    private static GameState gameState = GameState.getInstance();
    private static Random random = new Random();
    
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        server.createContext("/juego", new GameHandler());
        server.createContext("/status", new StatusHandler());
        server.createContext("/equipar", new EquiparHandler());
        server.createContext("/atacar", new AtacarHandler());
        server.createContext("/mover", new MoverHandler());
        server.createContext("/reset", new ResetHandler());
        server.createContext("/spawn-enemy", new SpawnEnemyHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("🗡️ Arena 2D - Servidor iniciado en puerto " + PORT);
        System.out.println("🌐 Abre http://localhost:" + PORT + "/juego en tu navegador");
        System.out.println("📋 Endpoints:");
        System.out.println("   GET  /juego - Arena 2D con imágenes");
        System.out.println("   GET  /status - Estado del héroe");
        System.out.println("   POST /equipar - Equipar ítems");
        System.out.println("   POST /atacar - Atacar enemigos");
        System.out.println("   POST /mover - Mover héroe");
        System.out.println("   POST /reset - Resetear juego");
        System.out.println("   POST /spawn-enemy - Generar enemigo 2D");
    }
    
    static class GameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = getArena2DHTML();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes("UTF-8"));
                os.close();
            }
        }
    }
    
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = gameState.getHeroeStatsJson();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
    
    static class EquiparHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                Map<String, String> params = parsePostBody(body);
                String item = params.get("item");
                
                try {
                    String resultado = gameState.equipar(item);
                    String response = "{\"message\":\"" + resultado + "\",\"stats\":" + gameState.getHeroeStatsJson() + "}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (Exception e) {
                    sendErrorResponse(exchange, 400, e.getMessage());
                }
            }
        }
    }
    
    static class AtacarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String[] nombres = {"Goblin", "Orco", "Esqueleto", "Demonio", "Dragón", "Troll", "Mago Oscuro", "Lobo", "Murciélago", "Fantasma"};
                    String nombre = nombres[random.nextInt(nombres.length)];
                    int vida = 3 + random.nextInt(4);
                    int critico = 1 + random.nextInt(3);
                    int escudo = 1 + random.nextInt(3);
                    int ataque = 2 + random.nextInt(3);
                    
                    Enemigo enemigo = new Enemigo(nombre, vida, critico, escudo, ataque);
                    gameState.setEnemigoActual(enemigo);
                    
                    MotorCombate.ResultadoCombate resultado = MotorCombate.simularCombate(gameState.getHeroe(), enemigo);
                    
                    StringBuilder jsonResponse = new StringBuilder();
                    jsonResponse.append("{");
                    jsonResponse.append("\"ganador\":\"").append(resultado.ganador).append("\",");
                    jsonResponse.append("\"turnos\":").append(resultado.turnos).append(",");
                    jsonResponse.append("\"detallesLog\":[");
                    
                    for (int i = 0; i < resultado.detailedCombatLog.size(); i++) {
                        if (i > 0) jsonResponse.append(",");
                        jsonResponse.append("\"").append(resultado.detailedCombatLog.get(i).replace("\"", "\\\"")).append("\"");
                    }
                    
                    jsonResponse.append("],");
                    jsonResponse.append("\"enemigo\":{");
                    jsonResponse.append("\"nombre\":\"").append(enemigo.getNombre()).append("\",");
                    jsonResponse.append("\"vida\":").append(enemigo.getVida()).append(",");
                    jsonResponse.append("\"maxVida\":").append(vida).append(",");
                    jsonResponse.append("\"ataque\":").append(enemigo.getAtaque()).append(",");
                    jsonResponse.append("\"escudo\":").append(enemigo.getEscudo()).append(",");
                    jsonResponse.append("\"critico\":").append(enemigo.getCritico());
                    jsonResponse.append("},");
                    jsonResponse.append("\"heroStats\":").append(gameState.getHeroeStatsJson());
                    jsonResponse.append("}");
                    
                    String response = jsonResponse.toString();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (Exception e) {
                    sendErrorResponse(exchange, 500, "Error al atacar: " + e.getMessage());
                }
            }
        }
    }
    
    static class MoverHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                Map<String, String> params = parsePostBody(body);
                
                try {
                    int dx = Integer.parseInt(params.get("dx"));
                    int dy = Integer.parseInt(params.get("dy"));
                    
                    gameState.moveHero(dx, dy);
                    
                    String response = "{\"message\":\"Héroe movido a posición (" + gameState.getHeroX() + "," + gameState.getHeroY() + ")\",\"stats\":" + gameState.getHeroeStatsJson() + "}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (Exception e) {
                    sendErrorResponse(exchange, 400, "Error al mover: " + e.getMessage());
                }
            }
        }
    }
    
    static class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                gameState.resetHeroe();
                String response = "{\"message\":\"Juego reseteado\",\"stats\":" + gameState.getHeroeStatsJson() + "}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
    
    static class SpawnEnemyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String[] nombres = {"Goblin", "Orco", "Esqueleto", "Demonio", "Dragón", "Troll", "Mago Oscuro", "Lobo", "Murciélago", "Fantasma"};
                    String nombre = nombres[random.nextInt(nombres.length)];
                    int vida = 2 + random.nextInt(6);
                    int critico = 1 + random.nextInt(4);
                    int escudo = 1 + random.nextInt(4);
                    int ataque = 1 + random.nextInt(5);
                    
                    Enemigo enemigo = new Enemigo(nombre, vida, critico, escudo, ataque);
                    gameState.setEnemigoActual(enemigo);
                    
                    String response = "{\"message\":\"Enemigo " + nombre + " generado\",\"enemigo\":{";
                    response += "\"nombre\":\"" + nombre + "\",";
                    response += "\"vida\":" + vida + ",";
                    response += "\"maxVida\":" + vida + ",";
                    response += "\"ataque\":" + ataque + ",";
                    response += "\"escudo\":" + escudo + ",";
                    response += "\"critico\":" + critico;
                    response += "}}";
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (Exception e) {
                    sendErrorResponse(exchange, 500, "Error al generar enemigo: " + e.getMessage());
                }
            }
        }
    }
    
    private static Map<String, String> parsePostBody(String body) {
        Map<String, String> params = new HashMap<>();
        if (body != null && !body.isEmpty()) {
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }
    
    private static void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String response = "{\"error\":\"" + message.replace("\"", "\\\"") + "\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    private static String getArena2DHTML() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>🗡️ ARENA 2D - Batalla Épica</title>\n" +
                "    <style>\n" +
                "        @import url('https://fonts.googleapis.com/css2?family=Press+Start+2P&display=swap');\n" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "        body {\n" +
                "            font-family: 'Press Start 2P', cursive;\n" +
                "            background: linear-gradient(135deg, #1a1a2e, #0f0f1e);\n" +
                "            color: #fff;\n" +
                "            min-height: 100vh;\n" +
                "            overflow-x: hidden;\n" +
                "        }\n" +
                "        .container { max-width: 1400px; margin: 0 auto; padding: 20px; }\n" +
                "        .header { text-align: center; margin-bottom: 30px; }\n" +
                "        .header h1 {\n" +
                "            font-size: 2.5rem;\n" +
                "            background: linear-gradient(45deg, #ff6b6b, #4ecdc4, #ffe66d);\n" +
                "            -webkit-background-clip: text;\n" +
                "            -webkit-text-fill-color: transparent;\n" +
                "            animation: glow 2s ease-in-out infinite alternate;\n" +
                "        }\n" +
                "        @keyframes glow { from { filter: brightness(1); } to { filter: brightness(1.2); } }\n" +
                "        .game-container { display: grid; grid-template-columns: 250px 800px 250px; gap: 20px; margin-bottom: 20px; }\n" +
                "        .panel {\n" +
                "            background: rgba(20,20,40,0.9);\n" +
                "            border: 3px solid #4ecdc4;\n" +
                "            border-radius: 15px;\n" +
                "            padding: 20px;\n" +
                "            box-shadow: 0 0 30px rgba(78,205,196,0.3);\n" +
                "        }\n" +
                "        .panel h3 { color: #ffe66d; margin-bottom: 15px; text-align: center; font-size: 0.9rem; }\n" +
                "        .game-arena {\n" +
                "            width: 800px; height: 600px;\n" +
                "            background: linear-gradient(45deg, #2d1b69, #0f0f1e),\n" +
                "                repeating-linear-gradient(0deg, transparent, transparent 20px, rgba(78,205,196,0.1) 20px, rgba(78,205,196,0.1) 21px),\n" +
                "                repeating-linear-gradient(90deg, transparent, transparent 20px, rgba(255,107,107,0.1) 20px, rgba(255,107,107,0.1) 21px);\n" +
                "            border: 4px solid #ff6b6b;\n" +
                "            border-radius: 20px;\n" +
                "            position: relative;\n" +
                "            overflow: hidden;\n" +
                "            box-shadow: 0 0 50px rgba(255,107,107,0.4), inset 0 0 50px rgba(78,205,196,0.1);\n" +
                "        }\n" +
                "        .hero-2d {\n" +
                "            position: absolute; width: 60px; height: 60px;\n" +
                "            background: linear-gradient(135deg, #ff6b6b, #ff4757);\n" +
                "            border: 3px solid #fff; border-radius: 10px;\n" +
                "            transition: all 0.2s ease; z-index: 10;\n" +
                "            display: flex; align-items: center; justify-content: center;\n" +
                "            font-size: 30px; box-shadow: 0 0 20px rgba(255,107,107,0.8);\n" +
                "        }\n" +
                "        .hero-2d::before { content: '🗡️'; }\n" +
                "        .hero-2d.sword-1 {\n" +
                "            background: linear-gradient(135deg, #f39c12, #e67e22);\n" +
                "            box-shadow: 0 0 25px rgba(243,156,18,0.9); animation: heroGlow1 2s infinite;\n" +
                "        }\n" +
                "        .hero-2d.sword-1::before { content: '⚔️'; }\n" +
                "        .hero-2d.sword-2 {\n" +
                "            background: linear-gradient(135deg, #9b59b6, #8e44ad);\n" +
                "            box-shadow: 0 0 30px rgba(155,89,182,1); animation: heroGlow2 1.5s infinite;\n" +
                "        }\n" +
                "        .hero-2d.sword-2::before { content: '🗡️'; }\n" +
                "        .hero-2d.sword-3 {\n" +
                "            background: linear-gradient(135deg, #e74c3c, #c0392b);\n" +
                "            box-shadow: 0 0 40px rgba(231,76,60,1); animation: heroGlow3 1s infinite;\n" +
                "        }\n" +
                "        .hero-2d.sword-3::before { content: '💀'; }\n" +
                "        @keyframes heroGlow1 { 0%, 100% { transform: scale(1) rotate(0deg); } 50% { transform: scale(1.1) rotate(5deg); } }\n" +
                "        @keyframes heroGlow2 { 0%, 100% { transform: scale(1) rotate(0deg); } 25% { transform: scale(1.2) rotate(10deg); } 75% { transform: scale(1.15) rotate(-10deg); } }\n" +
                "        @keyframes heroGlow3 { 0%, 100% { transform: scale(1) rotate(0deg); } 33% { transform: scale(1.3) rotate(15deg); } 66% { transform: scale(1.25) rotate(-15deg); } }\n" +
                "        .enemy-2d {\n" +
                "            position: absolute; width: 50px; height: 50px;\n" +
                "            background: linear-gradient(135deg, #8e44ad, #2c3e50);\n" +
                "            border: 2px solid #fff; border-radius: 8px;\n" +
                "            transition: all 0.3s ease; z-index: 5;\n" +
                "            display: flex; align-items: center; justify-content: center;\n" +
                "            font-size: 25px; box-shadow: 0 0 20px rgba(142,68,173,0.8);\n" +
                "            animation: enemyFloat 3s infinite ease-in-out;\n" +
                "        }\n" +
                "        @keyframes enemyFloat { 0%, 100% { transform: translateY(0px); } 50% { transform: translateY(-8px); } }\n" +
                "        .enemy-2d.goblin { background: linear-gradient(135deg, #27ae60, #229954); }\n" +
                "        .enemy-2d.goblin::before { content: '👺'; }\n" +
                "        .enemy-2d.orco { background: linear-gradient(135deg, #e74c3c, #c0392b); }\n" +
                "        .enemy-2d.orco::before { content: '👹'; }\n" +
                "        .enemy-2d.esqueleto { background: linear-gradient(135deg, #ecf0f1, #bdc3c7); }\n" +
                "        .enemy-2d.esqueleto::before { content: '💀'; }\n" +
                "        .enemy-2d.demonio { background: linear-gradient(135deg, #8e44ad, #6c3483); }\n" +
                "        .enemy-2d.demonio::before { content: '😈'; }\n" +
                "        .enemy-2d.dragon { background: linear-gradient(135deg, #e67e22, #d35400); }\n" +
                "        .enemy-2d.dragon::before { content: '🐉'; }\n" +
                "        .enemy-2d.troll { background: linear-gradient(135deg, #16a085, #138d75); }\n" +
                "        .enemy-2d.troll::before { content: '👾'; }\n" +
                "        .enemy-2d.mago { background: linear-gradient(135deg, #3498db, #2980b9); }\n" +
                "        .enemy-2d.mago::before { content: '🧙'; }\n" +
                "        .enemy-2d.lobo { background: linear-gradient(135deg, #7f8c8d, #5d6d7e); }\n" +
                "        .enemy-2d.lobo::before { content: '🐺'; }\n" +
                "        .enemy-2d.murcielago { background: linear-gradient(135deg, #34495e, #2c3e50); }\n" +
                "        .enemy-2d.murcielago::before { content: '🦇'; }\n" +
                "        .enemy-2d.fantasma { background: linear-gradient(135deg, #9b59b6, #8e44ad); }\n" +
                "        .enemy-2d.fantasma::before { content: '👻'; }\n" +
                "        .stat-item { display: flex; align-items: center; margin: 10px 0; background: rgba(78,205,196,0.1); padding: 8px; border-radius: 8px; border: 1px solid #4ecdc4; }\n" +
                "        .stat-label { font-size: 0.7rem; margin-right: 8px; min-width: 60px; }\n" +
                "        .stat-bar { flex: 1; height: 15px; background: rgba(0,0,0,0.5); border-radius: 8px; overflow: hidden; }\n" +
                "        .stat-fill { height: 100%; transition: width 0.5s ease; border-radius: 8px; }\n" +
                "        .vida-fill { background: linear-gradient(90deg, #ff6b6b, #ff4757); }\n" +
                "        .escudo-fill { background: linear-gradient(90deg, #4ecdc4, #16a085); }\n" +
                "        .ataque-fill { background: linear-gradient(90deg, #ffe66d, #f39c12); }\n" +
                "        .critico-fill { background: linear-gradient(90deg, #a8e6cf, #7fcdbb); }\n" +
                "        .stat-value { margin-left: 8px; font-size: 0.7rem; font-weight: bold; }\n" +
                "        .controls { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin: 15px 0; }\n" +
                "        .btn {\n" +
                "            padding: 10px 8px; border: 2px solid; border-radius: 8px;\n" +
                "            background: rgba(0,0,0,0.8); color: #fff;\n" +
                "            font-family: 'Press Start 2P', cursive; font-size: 0.6rem;\n" +
                "            cursor: pointer; transition: all 0.3s ease; text-transform: uppercase;\n" +
                "        }\n" +
                "        .btn-equipar { border-color: #4ecdc4; color: #4ecdc4; }\n" +
                "        .btn-equipar:hover { background: rgba(78,205,196,0.2); transform: translateY(-2px); }\n" +
                "        .btn-accion { border-color: #ff6b6b; color: #ff6b6b; }\n" +
                "        .btn-accion:hover { background: rgba(255,107,107,0.2); transform: translateY(-2px); }\n" +
                "        .btn-especial { border-color: #ffe66d; color: #ffe66d; }\n" +
                "        .btn-especial:hover { background: rgba(255,230,109,0.2); transform: translateY(-2px); }\n" +
                "        .btn-reset { border-color: #a8e6cf; color: #a8e6cf; }\n" +
                "        .btn-reset:hover { background: rgba(168,230,207,0.2); transform: translateY(-2px); }\n" +
                "        .equipment-display { background: rgba(255,230,109,0.1); padding: 10px; border-radius: 8px; margin: 10px 0; border: 1px solid #ffe66d; }\n" +
                "        .equipment-display strong { color: #ffe66d; display: block; margin-bottom: 8px; font-size: 0.7rem; }\n" +
                "        .equipment-item { display: inline-block; padding: 4px 8px; margin: 3px; background: rgba(255,107,107,0.2); border: 1px solid #ff6b6b; border-radius: 12px; font-size: 0.6rem; color: #ff6b6b; }\n" +
                "        .enemy-info { background: rgba(142,68,173,0.1); padding: 10px; border-radius: 8px; margin: 10px 0; border: 1px solid #8e44ad; text-align: center; }\n" +
                "        .enemy-info h4 { color: #8e44ad; margin-bottom: 8px; font-size: 0.7rem; }\n" +
                "        .enemy-info div { font-size: 0.6rem; line-height: 1.4; }\n" +
                "        .combat-log { background: rgba(0,0,0,0.8); padding: 15px; border-radius: 10px; max-height: 150px; overflow-y: auto; border: 2px solid #4ecdc4; }\n" +
                "        .combat-log h4 { color: #ffe66d; margin-bottom: 10px; text-align: center; font-size: 0.7rem; }\n" +
                "        .combat-entry { padding: 6px 8px; margin: 5px 0; border-left: 3px solid #4ecdc4; background: rgba(78,205,196,0.05); font-size: 0.6rem; border-radius: 4px; }\n" +
                "        .combat-entry.critical { border-left-color: #ff6b6b; background: rgba(255,107,107,0.1); color: #ff6b6b; }\n" +
                "        .combat-entry.enemy { border-left-color: #ffe66d; background: rgba(255,230,109,0.1); color: #ffe66d; }\n" +
                "        .combat-entry.victory { border-left-color: #a8e6cf; background: rgba(168,230,207,0.1); color: #a8e6cf; }\n" +
                "        .message { padding: 12px; margin: 10px 0; border-radius: 8px; text-align: center; font-size: 0.7rem; animation: slideIn 0.3s ease-out; }\n" +
                "        .success { background: rgba(168,230,207,0.2); border: 2px solid #a8e6cf; color: #a8e6cf; }\n" +
                "        .error { background: rgba(255,107,107,0.2); border: 2px solid #ff6b6b; color: #ff6b6b; }\n" +
                "        .info { background: rgba(78,205,196,0.2); border: 2px solid #4ecdc4; color: #4ecdc4; }\n" +
                "        .warning { background: rgba(255,230,109,0.2); border: 2px solid #ffe66d; color: #ffe66d; }\n" +
                "        @keyframes slideIn { from { transform: translateY(-10px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }\n" +
                "        .collision-effect {\n" +
                "            position: absolute; width: 100px; height: 100px; border-radius: 50%;\n" +
                "            background: radial-gradient(circle, rgba(255,255,255,0.9), rgba(255,107,107,0.6), transparent);\n" +
                "            animation: collisionPulse 0.8s ease-out; pointer-events: none; z-index: 20;\n" +
                "        }\n" +
                "        @keyframes collisionPulse { 0% { transform: scale(0); opacity: 1; } 50% { transform: scale(1.5); opacity: 0.7; } 100% { transform: scale(3); opacity: 0; } }\n" +
                "        .instructions { background: rgba(20,20,40,0.9); padding: 15px; border-radius: 10px; margin-bottom: 20px; border: 2px solid #4ecdc4; }\n" +
                "        .instructions h3 { color: #ffe66d; text-align: center; margin-bottom: 10px; font-size: 0.8rem; }\n" +
                "        .instructions p { margin: 5px 0; font-size: 0.6rem; color: #4ecdc4; }\n" +
                "        ::-webkit-scrollbar { width: 6px; }\n" +
                "        ::-webkit-scrollbar-track { background: rgba(0,0,0,0.3); }\n" +
                "        ::-webkit-scrollbar-thumb { background: #4ecdc4; border-radius: 3px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\"><h1>🗡️ ARENA 2D - BATALLA ÉPICA</h1></div>\n" +
                "        <div class=\"instructions\">\n" +
                "            <h3>🎮 CONTROLES</h3>\n" +
                "            <p>• WASD/FLECHAS - Mover héroe</p>\n" +
                "            <p>• ESPACIO - Atacar enemigo cercano</p>\n" +
                "            <p>• CLICK - Equipar ítems</p>\n" +
                "            <p>• CHOCAR - Pelea automática</p>\n" +
                "        </div>\n" +
                "        <div class=\"game-container\">\n" +
                "            <div class=\"panel\">\n" +
                "                <h3>📊 STATS HÉROE</h3>\n" +
                "                <div class=\"stat-item\"><span class=\"stat-label\">❤️ VIDA</span><div class=\"stat-bar\"><div class=\"stat-fill vida-fill\" id=\"vidaBar\"></div></div><span class=\"stat-value\" id=\"vidaValue\">3/5</span></div>\n" +
                "                <div class=\"stat-item\"><span class=\"stat-label\">🛡️ ESCUDO</span><div class=\"stat-bar\"><div class=\"stat-fill escudo-fill\" id=\"escudoBar\"></div></div><span class=\"stat-value\" id=\"escudoValue\">1/5</span></div>\n" +
                "                <div class=\"stat-item\"><span class=\"stat-label\">⚔️ ATAQUE</span><div class=\"stat-bar\"><div class=\"stat-fill ataque-fill\" id=\"ataqueBar\"></div></div><span class=\"stat-value\" id=\"ataqueValue\">2/5</span></div>\n" +
                "                <div class=\"stat-item\"><span class=\"stat-label\">💥 CRÍTICO</span><div class=\"stat-bar\"><div class=\"stat-fill critico-fill\" id=\"criticoBar\"></div></div><span class=\"stat-value\" id=\"criticoValue\">2/5</span></div>\n" +
                "                <div class=\"equipment-display\"><strong>🎒 EQUIPAMIENTO</strong><div id=\"equipmentList\">Ninguno</div></div>\n" +
                "                <div class=\"enemy-info\"><h4>👹 ENEMIGO ACTUAL</h4><div id=\"enemyInfo\">No hay enemigo</div></div>\n" +
                "            </div>\n" +
                "            <div class=\"panel\">\n" +
                "                <h3>🏟️ ARENA DE BATALLA</h3>\n" +
                "                <div class=\"game-arena\" id=\"gameArena\"><div class=\"hero-2d\" id=\"hero\"></div></div>\n" +
                "                <div id=\"messageArea\"></div>\n" +
                "            </div>\n" +
                "            <div class=\"panel\">\n" +
                "                <h3>⚔️ COMBATE</h3>\n" +
                "                <div class=\"controls\">\n" +
                "                    <button class=\"btn btn-equipar\" onclick=\"equipar('espada')\">⚔️ ESPADA</button>\n" +
                "                    <button class=\"btn btn-equipar\" onclick=\"equipar('escudo')\">🛡️ ESCUDO</button>\n" +
                "                    <button class=\"btn btn-equipar\" onclick=\"equipar('poder')\">💪 PODER</button>\n" +
                "                    <button class=\"btn btn-especial\" onclick=\"equipar('buffataque')\">🔥 FUEGO</button>\n" +
                "                    <button class=\"btn btn-especial\" onclick=\"equipar('buffmultiplicador')\">⚡ RAYO</button>\n" +
                "                    <button class=\"btn btn-especial\" onclick=\"spawnEnemy()\">👹 GENERAR</button>\n" +
                "                    <button class=\"btn btn-accion\" onclick=\"atacar()\">⚔️ ATACAR</button>\n" +
                "                    <button class=\"btn btn-especial\" onclick=\"specialAttack()\">🌟 ESPECIAL</button>\n" +
                "                    <button class=\"btn btn-reset\" onclick=\"reset()\">🔄 RESET</button>\n" +
                "                </div>\n" +
                "                <div class=\"combat-log\"><h4>📜 REGISTRO</h4><div id=\"combatLog\">Esperando combate...</div></div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    <script>\n" +
                "        let heroX = 370, heroY = 270, currentEnemy = null;\n" +
                "        const moveSpeed = 25, gameArenaSize = { width: 800, height: 600 }, heroSize = 60, enemySize = 50;\n" +
                "        document.addEventListener('DOMContentLoaded', () => { updateStats(); updateHeroPosition(); setupControls(); startGameLoop(); });\n" +
                "        function setupControls() {\n" +
                "            document.addEventListener('keydown', (event) => {\n" +
                "                let dx = 0, dy = 0;\n" +
                "                switch(event.key.toLowerCase()) {\n" +
                "                    case 'w': case 'arrowup': dy = -moveSpeed; break;\n" +
                "                    case 's': case 'arrowdown': dy = moveSpeed; break;\n" +
                "                    case 'a': case 'arrowleft': dx = -moveSpeed; break;\n" +
                "                    case 'd': case 'arrowright': dx = moveSpeed; break;\n" +
                "                    case ' ': event.preventDefault(); checkAndAttack(); return;\n" +
                "                    default: return;\n" +
                "                }\n" +
                "                event.preventDefault(); moveHero(dx, dy);\n" +
                "            });\n" +
                "        }\n" +
                "        function moveHero(dx, dy) {\n" +
                "            const newX = Math.max(0, Math.min(heroX + dx, gameArenaSize.width - heroSize));\n" +
                "            const newY = Math.max(0, Math.min(heroY + dy, gameArenaSize.height - heroSize));\n" +
                "            fetch('/mover', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: `dx=${dx}&dy=${dy}` })\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                if (data.message) { heroX = newX; heroY = newY; updateHeroPosition(); updateStatsDisplay(data.stats); checkCollisions(); }\n" +
                "                else if (data.error) showMessage(data.error, 'error');\n" +
                "            })\n" +
                "            .catch(error => showMessage('Error: ' + error, 'error'));\n" +
                "        }\n" +
                "        function updateHeroPosition() { const hero = document.getElementById('hero'); hero.style.left = heroX + 'px'; hero.style.top = heroY + 'px'; }\n" +
                "        function spawnEnemy() {\n" +
                "            fetch('/spawn-enemy', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                if (data.message) { showMessage(data.message, 'info'); addEnemyToArena(data.enemigo); }\n" +
                "                else if (data.error) showMessage(data.error, 'error');\n" +
                "            })\n" +
                "            .catch(error => showMessage('Error: ' + error, 'error'));\n" +
                "        }\n" +
                "        function addEnemyToArena(enemyData) {\n" +
                "            const gameArena = document.getElementById('gameArena');\n" +
                "            const existingEnemy = document.querySelector('.enemy-2d'); if (existingEnemy) existingEnemy.remove();\n" +
                "            const enemy = document.createElement('div'); enemy.className = 'enemy-2d'; enemy.id = 'enemy';\n" +
                "            const enemyType = enemyData.nombre.toLowerCase();\n" +
                "            if (enemyType.includes('goblin')) enemy.classList.add('goblin');\n" +
                "            else if (enemyType.includes('orco')) enemy.classList.add('orco');\n" +
                "            else if (enemyType.includes('esqueleto')) enemy.classList.add('esqueleto');\n" +
                "            else if (enemyType.includes('demonio')) enemy.classList.add('demonio');\n" +
                "            else if (enemyType.includes('dragón') || enemyType.includes('dragon')) enemy.classList.add('dragon');\n" +
                "            else if (enemyType.includes('troll')) enemy.classList.add('troll');\n" +
                "            else if (enemyType.includes('mago')) enemy.classList.add('mago');\n" +
                "            else if (enemyType.includes('lobo')) enemy.classList.add('lobo');\n" +
                "            else if (enemyType.includes('murciélago') || enemyType.includes('murcielago')) enemy.classList.add('murcielago');\n" +
                "            else if (enemyType.includes('fantasma')) enemy.classList.add('fantasma');\n" +
                "            const enemyX = Math.random() * (gameArenaSize.width - enemySize);\n" +
                "            const enemyY = Math.random() * (gameArenaSize.height - enemySize);\n" +
                "            enemy.style.left = enemyX + 'px'; enemy.style.top = enemyY + 'px';\n" +
                "            gameArena.appendChild(enemy);\n" +
                "            currentEnemy = { element: enemy, x: enemyX, y: enemyY, data: enemyData };\n" +
                "            updateEnemyInfo(enemyData);\n" +
                "        }\n" +
                "        function updateEnemyInfo(enemyData) {\n" +
                "            const enemyInfo = document.getElementById('enemyInfo');\n" +
                "            enemyInfo.innerHTML = `<strong>${enemyData.nombre}</strong><br>❤️ ${enemyData.vida}/${enemyData.maxVida}<br>⚔️ ${enemyData.ataque} 🛡️ ${enemyData.escudo}<br>💥 ${enemyData.critico}% crítico`;\n" +
                "        }\n" +
                "        function checkCollisions() {\n" +
                "            if (!currentEnemy) return;\n" +
                "            const distance = Math.sqrt(Math.pow(heroX - currentEnemy.x, 2) + Math.pow(heroY - currentEnemy.y, 2));\n" +
                "            if (distance < (heroSize + enemySize) / 2) { showCollisionEffect(heroX + heroSize/2, heroY + heroSize/2); performRandomCombat(); }\n" +
                "        }\n" +
                "        function showCollisionEffect(x, y) {\n" +
                "            const gameArena = document.getElementById('gameArena');\n" +
                "            const effect = document.createElement('div'); effect.className = 'collision-effect';\n" +
                "            effect.style.left = (x - 50) + 'px'; effect.style.top = (y - 50) + 'px';\n" +
                "            gameArena.appendChild(effect); setTimeout(() => effect.remove(), 800);\n" +
                "        }\n" +
                "        function performRandomCombat() {\n" +
                "            if (!currentEnemy) return;\n" +
                "            fetch('/atacar', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                if (data.ganador) { displayCombatResults(data); updateStatsDisplay(data.heroStats); if (data.ganador === 'HeroeBase') removeEnemy(); }\n" +
                "                else if (data.error) showMessage(data.error, 'error');\n" +
                "            })\n" +
                "            .catch(error => showMessage('Error: ' + error, 'error'));\n" +
                "        }\n" +
                "        function displayCombatResults(data) {\n" +
                "            const combatLog = document.getElementById('combatLog');\n" +
                "            const resultEntry = document.createElement('div');\n" +
                "            resultEntry.className = 'combat-entry ' + (data.ganador === 'HeroeBase' ? 'victory' : 'enemy');\n" +
                "            resultEntry.innerHTML = `<strong>${data.ganador === 'HeroeBase' ? '🎉 VICTORIA' : '💀 DERROTA'}</strong> vs ${data.enemigo.nombre}`;\n" +
                "            combatLog.insertBefore(resultEntry, combatLog.firstChild);\n" +
                "            if (data.detallesLog && data.detallesLog.length > 0) {\n" +
                "                data.detallesLog.forEach(entry => {\n" +
                "                    const logEntry = document.createElement('div'); logEntry.className = 'combat-entry';\n" +
                "                    if (entry.includes('Crítico')) logEntry.className += ' critical';\n" +
                "                    else if (entry.includes('contraatac')) logEntry.className += ' enemy';\n" +
                "                    logEntry.textContent = entry; combatLog.insertBefore(logEntry, combatLog.firstChild);\n" +
                "                });\n" +
                "            }\n" +
                "            showMessage(`${data.ganador} ganó en ${data.turnos} turnos`, data.ganador === 'HeroeBase' ? 'success' : 'error');\n" +
                "        }\n" +
                "        function removeEnemy() {\n" +
                "            if (currentEnemy && currentEnemy.element) { currentEnemy.element.remove(); currentEnemy = null; document.getElementById('enemyInfo').innerHTML = 'No hay enemigo'; }\n" +
                "        }\n" +
                "        function checkAndAttack() {\n" +
                "            if (currentEnemy) {\n" +
                "                const distance = Math.sqrt(Math.pow(heroX - currentEnemy.x, 2) + Math.pow(heroY - currentEnemy.y, 2));\n" +
                "                if (distance < 150) performRandomCombat();\n" +
                "                else showMessage('¡Acércate al enemigo!', 'warning');\n" +
                "            } else showMessage('¡Genera un enemigo primero!', 'warning');\n" +
                "        }\n" +
                "        function specialAttack() {\n" +
                "            const abilities = ['FUEGO SAGRADO', 'RAYO DIVINO', 'HIELO ARCANO', 'TORMENTA SOMBRA'];\n" +
                "            const ability = abilities[Math.floor(Math.random() * abilities.length)];\n" +
                "            showMessage(`🌟 ${ability} ACTIVADO!`, 'info'); equipar('buffataque');\n" +
                "            if (currentEnemy) setTimeout(() => performRandomCombat(), 500);\n" +
                "        }\n" +
                "        function updateStats() {\n" +
                "            fetch('/status').then(response => response.json()).then(data => updateStatsDisplay(data)).catch(error => showMessage('Error: ' + error, 'error'));\n" +
                "        }\n" +
                "        function updateStatsDisplay(stats) {\n" +
                "            const maxStat = 5;\n" +
                "            updateBar('vida', stats.vida, maxStat); updateBar('escudo', stats.escudo, maxStat);\n" +
                "            updateBar('ataque', stats.ataque, maxStat); updateBar('critico', stats.critico, maxStat);\n" +
                "            const equipmentList = document.getElementById('equipmentList');\n" +
                "            if (stats.decoradores && stats.decoradores.length > 0) {\n" +
                "                equipmentList.innerHTML = stats.decoradores.map(d => `<span class=\"equipment-item\">${d}</span>`).join('');\n" +
                "            } else equipmentList.innerHTML = '<span class=\"equipment-item\">Ninguno</span>';\n" +
                "            updateHeroVisual(stats);\n" +
                "        }\n" +
                "        function updateHeroVisual(stats) {\n" +
                "            const hero = document.getElementById('hero'); hero.classList.remove('sword-1', 'sword-2', 'sword-3');\n" +
                "            let swordCount = 0; if (stats.decoradores) swordCount = stats.decoradores.filter(d => d === 'Espada').length;\n" +
                "            if (swordCount >= 1) hero.classList.add('sword-' + swordCount);\n" +
                "        }\n" +
                "        function updateBar(statName, value, maxValue) {\n" +
                "            const bar = document.getElementById(statName + 'Bar'); const valueSpan = document.getElementById(statName + 'Value');\n" +
                "            const percentage = (value / maxValue) * 100; bar.style.width = percentage + '%'; valueSpan.textContent = value + '/' + maxValue;\n" +
                "        }\n" +
                "        function equipar(item) {\n" +
                "            fetch('/equipar', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: `item=${item}` })\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                if (data.message) { showMessage(data.message, 'success'); updateStatsDisplay(data.stats); }\n" +
                "                else if (data.error) showMessage(data.error, 'error');\n" +
                "            })\n" +
                "            .catch(error => showMessage('Error: ' + error, 'error'));\n" +
                "        }\n" +
                "        function atacar() { checkAndAttack(); }\n" +
                "        function reset() {\n" +
                "            if (confirm('¿Resetear juego?')) {\n" +
                "                fetch('/reset', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })\n" +
                "                .then(response => response.json())\n" +
                "                .then(data => {\n" +
                "                    if (data.message) {\n" +
                "                        showMessage(data.message, 'success'); updateStatsDisplay(data.stats);\n" +
                "                        heroX = 370; heroY = 270; updateHeroPosition(); removeEnemy();\n" +
                "                        document.getElementById('combatLog').innerHTML = 'Esperando combate...';\n" +
                "                    } else if (data.error) showMessage(data.error, 'error');\n" +
                "                })\n" +
                "                .catch(error => showMessage('Error: ' + error, 'error'));\n" +
                "            }\n" +
                "        }\n" +
                "        function showMessage(message, type) {\n" +
                "            const messageArea = document.getElementById('messageArea'); const messageDiv = document.createElement('div');\n" +
                "            messageDiv.className = `message ${type}`; messageDiv.textContent = message;\n" +
                "            messageArea.innerHTML = ''; messageArea.appendChild(messageDiv);\n" +
                "            setTimeout(() => { messageDiv.style.opacity = '0'; setTimeout(() => { if (messageDiv.parentNode) messageDiv.parentNode.removeChild(messageDiv); }, 300); }, 3000);\n" +
                "        }\n" +
                "        function startGameLoop() { setInterval(() => checkCollisions(), 100); setInterval(updateStats, 5000); }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
