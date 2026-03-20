import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class JuegoServidorV2 {
    private static final int PORT = 8080;
    private static GameState gameState = GameState.getInstance();
    private static Random random = new Random();
    
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Endpoints
        server.createContext("/juego", new GameHandler());
        server.createContext("/status", new StatusHandler());
        server.createContext("/equipar", new EquiparHandler());
        server.createContext("/atacar", new AtacarHandler());
        server.createContext("/mover", new MoverHandler());
        server.createContext("/reset", new ResetHandler());
        server.createContext("/spawn-enemy", new SpawnEnemyHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("🎮 Servidor de Juego de Héroes iniciado en puerto " + PORT);
        System.out.println("🌐 Abre http://localhost:" + PORT + "/juego en tu navegador");
        System.out.println("📋 Endpoints disponibles:");
        System.out.println("   GET  /juego - Interfaz del juego");
        System.out.println("   GET  /status - Estado del héroe");
        System.out.println("   POST /equipar - Equipar ítems");
        System.out.println("   POST /atacar - Atacar enemigos");
        System.out.println("   POST /mover - Mover héroe");
        System.out.println("   POST /reset - Resetear juego");
        System.out.println("   POST /spawn-enemy - Generar enemigo aleatorio");
    }
    
    static class GameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = getEnhancedGameHTML();
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
                    // Generar enemigo aleatorio
                    String[] nombres = {"Goblin", "Orco", "Esqueleto", "Demonio", "Dragón", "Troll", "Mago Oscuro"};
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
    
    private static String getEnhancedGameHTML() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>🎮 Juego de Héroes 2D</title>\n" +
                "    <style>\n" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "        body {\n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
                "            color: white;\n" +
                "            min-height: 100vh;\n" +
                "            overflow-x: hidden;\n" +
                "        }\n" +
                "        .container {\n" +
                "            max-width: 1400px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .header {\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 30px;\n" +
                "            animation: glow 2s ease-in-out infinite alternate;\n" +
                "        }\n" +
                "        @keyframes glow {\n" +
                "            from { text-shadow: 0 0 10px #fff, 0 0 20px #fff, 0 0 30px #e60073; }\n" +
                "            to { text-shadow: 0 0 20px #fff, 0 0 30px #ff4da6, 0 0 40px #ff4da6; }\n" +
                "        }\n" +
                "        .game-container {\n" +
                "            display: grid;\n" +
                "            grid-template-columns: 1fr 2fr 1fr;\n" +
                "            gap: 20px;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .panel {\n" +
                "            background: rgba(255,255,255,0.1);\n" +
                "            backdrop-filter: blur(10px);\n" +
                "            border-radius: 20px;\n" +
                "            padding: 20px;\n" +
                "            border: 2px solid rgba(255,255,255,0.2);\n" +
                "            box-shadow: 0 8px 32px rgba(0,0,0,0.1);\n" +
                "        }\n" +
                "        .game-area {\n" +
                "            width: 600px;\n" +
                "            height: 400px;\n" +
                "            background: linear-gradient(45deg, #1a1a2e, #16213e);\n" +
                "            border: 3px solid #fff;\n" +
                "            border-radius: 15px;\n" +
                "            position: relative;\n" +
                "            overflow: hidden;\n" +
                "            box-shadow: 0 0 30px rgba(255,255,255,0.3);\n" +
                "        }\n" +
                "        .hero {\n" +
                "            width: 40px;\n" +
                "            height: 40px;\n" +
                "            background: radial-gradient(circle, #ff6b6b, #ff0000);\n" +
                "            border: 3px solid #fff;\n" +
                "            border-radius: 50%;\n" +
                "            position: absolute;\n" +
                "            transition: all 0.3s ease;\n" +
                "            z-index: 10;\n" +
                "            box-shadow: 0 0 20px rgba(255,107,107,0.8);\n" +
                "        }\n" +
                "        .hero.sword-1 {\n" +
                "            background: radial-gradient(circle, #ff6b6b, #ff8e8e);\n" +
                "            box-shadow: 0 0 25px rgba(255,107,107,0.9), 0 0 40px rgba(255,215,0,0.4);\n" +
                "            animation: heroGlow1 2s ease-in-out infinite alternate;\n" +
                "        }\n" +
                "        .hero.sword-2 {\n" +
                "            background: radial-gradient(circle, #ff6b6b, #ffd700);\n" +
                "            box-shadow: 0 0 30px rgba(255,107,107,1), 0 0 50px rgba(255,215,0,0.6);\n" +
                "            animation: heroGlow2 1.5s ease-in-out infinite alternate;\n" +
                "        }\n" +
                "        .hero.sword-3 {\n" +
                "            background: radial-gradient(circle, #ff0000, #ff6b6b);\n" +
                "            box-shadow: 0 0 40px rgba(255,0,0,1), 0 0 60px rgba(255,215,0,0.8), 0 0 80px rgba(255,107,107,0.6);\n" +
                "            animation: heroGlow3 1s ease-in-out infinite alternate;\n" +
                "        }\n" +
                "        @keyframes heroGlow1 {\n" +
                "            from { transform: scale(1); filter: brightness(1); }\n" +
                "            to { transform: scale(1.1); filter: brightness(1.2); }\n" +
                "        }\n" +
                "        @keyframes heroGlow2 {\n" +
                "            from { transform: scale(1) rotate(0deg); filter: brightness(1); }\n" +
                "            to { transform: scale(1.2) rotate(5deg); filter: brightness(1.3); }\n" +
                "        }\n" +
                "        @keyframes heroGlow3 {\n" +
                "            from { transform: scale(1) rotate(0deg); filter: brightness(1) hue-rotate(0deg); }\n" +
                "            to { transform: scale(1.3) rotate(10deg); filter: brightness(1.5) hue-rotate(20deg); }\n" +
                "        }\n" +
                "        .enemy {\n" +
                "            width: 35px;\n" +
                "            height: 35px;\n" +
                "            background: radial-gradient(circle, #9b59b6, #8e44ad);\n" +
                "            border: 2px solid #fff;\n" +
                "            border-radius: 50%;\n" +
                "            position: absolute;\n" +
                "            transition: all 0.3s ease;\n" +
                "            z-index: 5;\n" +
                "            box-shadow: 0 0 15px rgba(155,89,182,0.8);\n" +
                "        }\n" +
                "        .stats-display {\n" +
                "            margin: 10px 0;\n" +
                "        }\n" +
                "        .stat-item {\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            margin: 8px 0;\n" +
                "            background: rgba(255,255,255,0.05);\n" +
                "            padding: 10px;\n" +
                "            border-radius: 10px;\n" +
                "            transition: all 0.3s ease;\n" +
                "        }\n" +
                "        .stat-item:hover {\n" +
                "            background: rgba(255,255,255,0.1);\n" +
                "            transform: translateX(5px);\n" +
                "        }\n" +
                "        .stat-label {\n" +
                "            font-weight: bold;\n" +
                "            margin-right: 10px;\n" +
                "            min-width: 80px;\n" +
                "        }\n" +
                "        .stat-bar {\n" +
                "            flex: 1;\n" +
                "            height: 20px;\n" +
                "            background: rgba(255,255,255,0.2);\n" +
                "            border-radius: 10px;\n" +
                "            overflow: hidden;\n" +
                "            position: relative;\n" +
                "        }\n" +
                "        .stat-fill {\n" +
                "            height: 100%;\n" +
                "            transition: width 0.5s ease, background 0.3s ease;\n" +
                "            border-radius: 10px;\n" +
                "        }\n" +
                "        .vida-fill { background: linear-gradient(90deg, #e74c3c, #c0392b); }\n" +
                "        .escudo-fill { background: linear-gradient(90deg, #3498db, #2980b9); }\n" +
                "        .ataque-fill { background: linear-gradient(90deg, #f39c12, #e67e22); }\n" +
                "        .critico-fill { background: linear-gradient(90deg, #2ecc71, #27ae60); }\n" +
                "        .stat-value {\n" +
                "            margin-left: 10px;\n" +
                "            font-weight: bold;\n" +
                "            min-width: 40px;\n" +
                "            text-align: right;\n" +
                "        }\n" +
                "        .controls {\n" +
                "            display: grid;\n" +
                "            grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));\n" +
                "            gap: 10px;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        .btn {\n" +
                "            padding: 12px 20px;\n" +
                "            border: none;\n" +
                "            border-radius: 12px;\n" +
                "            font-weight: bold;\n" +
                "            cursor: pointer;\n" +
                "            transition: all 0.3s ease;\n" +
                "            text-transform: uppercase;\n" +
                "            font-size: 12px;\n" +
                "            position: relative;\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "        .btn::before {\n" +
                "            content: '';\n" +
                "            position: absolute;\n" +
                "            top: 0;\n" +
                "            left: -100%;\n" +
                "            width: 100%;\n" +
                "            height: 100%;\n" +
                "            background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);\n" +
                "            transition: left 0.5s;\n" +
                "        }\n" +
                "        .btn:hover::before {\n" +
                "            left: 100%;\n" +
                "        }\n" +
                "        .btn-equipar {\n" +
                "            background: linear-gradient(135deg, #3498db, #2980b9);\n" +
                "            color: white;\n" +
                "            box-shadow: 0 4px 15px rgba(52,152,219,0.3);\n" +
                "        }\n" +
                "        .btn-equipar:hover {\n" +
                "            transform: translateY(-2px);\n" +
                "            box-shadow: 0 6px 20px rgba(52,152,219,0.4);\n" +
                "        }\n" +
                "        .btn-accion {\n" +
                "            background: linear-gradient(135deg, #e74c3c, #c0392b);\n" +
                "            color: white;\n" +
                "            box-shadow: 0 4px 15px rgba(231,76,60,0.3);\n" +
                "        }\n" +
                "        .btn-accion:hover {\n" +
                "            transform: translateY(-2px);\n" +
                "            box-shadow: 0 6px 20px rgba(231,76,60,0.4);\n" +
                "        }\n" +
                "        .btn-especial {\n" +
                "            background: linear-gradient(135deg, #9b59b6, #8e44ad);\n" +
                "            color: white;\n" +
                "            box-shadow: 0 4px 15px rgba(155,89,182,0.3);\n" +
                "        }\n" +
                "        .btn-especial:hover {\n" +
                "            transform: translateY(-2px);\n" +
                "            box-shadow: 0 6px 20px rgba(155,89,182,0.4);\n" +
                "        }\n" +
                "        .btn-reset {\n" +
                "            background: linear-gradient(135deg, #95a5a6, #7f8c8d);\n" +
                "            color: white;\n" +
                "            box-shadow: 0 4px 15px rgba(149,165,166,0.3);\n" +
                "        }\n" +
                "        .btn-reset:hover {\n" +
                "            transform: translateY(-2px);\n" +
                "            box-shadow: 0 6px 20px rgba(149,165,166,0.4);\n" +
                "        }\n" +
                "        .message {\n" +
                "            padding: 15px;\n" +
                "            margin: 10px 0;\n" +
                "            border-radius: 12px;\n" +
                "            text-align: center;\n" +
                "            animation: slideIn 0.5s ease-out;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .success { background: linear-gradient(135deg, #2ecc71, #27ae60); color: white; }\n" +
                "        .error { background: linear-gradient(135deg, #e74c3c, #c0392b); color: white; }\n" +
                "        .info { background: linear-gradient(135deg, #3498db, #2980b9); color: white; }\n" +
                "        .warning { background: linear-gradient(135deg, #f39c12, #e67e22); color: white; }\n" +
                "        @keyframes slideIn {\n" +
                "            from { transform: translateY(-20px); opacity: 0; }\n" +
                "            to { transform: translateY(0); opacity: 1; }\n" +
                "        }\n" +
                "        .combat-log {\n" +
                "            background: rgba(0,0,0,0.4);\n" +
                "            padding: 20px;\n" +
                "            border-radius: 15px;\n" +
                "            margin: 20px 0;\n" +
                "            max-height: 250px;\n" +
                "            overflow-y: auto;\n" +
                "            border: 2px solid rgba(255,255,255,0.1);\n" +
                "        }\n" +
                "        .combat-entry {\n" +
                "            padding: 8px 12px;\n" +
                "            margin: 5px 0;\n" +
                "            border-radius: 8px;\n" +
                "            border-left: 4px solid #3498db;\n" +
                "            background: rgba(255,255,255,0.05);\n" +
                "            animation: slideIn 0.3s ease-out;\n" +
                "            font-size: 14px;\n" +
                "        }\n" +
                "        .combat-entry.critical {\n" +
                "            border-left-color: #e74c3c;\n" +
                "            background: rgba(231,76,60,0.1);\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .combat-entry.enemy {\n" +
                "            border-left-color: #f39c12;\n" +
                "            background: rgba(243,156,18,0.1);\n" +
                "        }\n" +
                "        .combat-entry.victory {\n" +
                "            border-left-color: #2ecc71;\n" +
                "            background: rgba(46,204,113,0.1);\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .equipment-display {\n" +
                "            background: rgba(255,255,255,0.1);\n" +
                "            padding: 15px;\n" +
                "            border-radius: 12px;\n" +
                "            margin: 15px 0;\n" +
                "        }\n" +
                "        .equipment-item {\n" +
                "            display: inline-block;\n" +
                "            padding: 5px 10px;\n" +
                "            margin: 3px;\n" +
                "            background: rgba(255,255,255,0.2);\n" +
                "            border-radius: 15px;\n" +
                "            font-size: 12px;\n" +
                "            border: 1px solid rgba(255,255,255,0.3);\n" +
                "        }\n" +
                "        .instructions {\n" +
                "            background: rgba(255,255,255,0.1);\n" +
                "            padding: 20px;\n" +
                "            border-radius: 15px;\n" +
                "            margin-bottom: 20px;\n" +
                "            border: 2px solid rgba(255,255,255,0.2);\n" +
                "        }\n" +
                "        .enemy-info {\n" +
                "            background: rgba(155,89,182,0.2);\n" +
                "            padding: 15px;\n" +
                "            border-radius: 12px;\n" +
                "            margin: 15px 0;\n" +
                "            border: 2px solid rgba(155,89,182,0.4);\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .collision-effect {\n" +
                "            position: absolute;\n" +
                "            width: 60px;\n" +
                "            height: 60px;\n" +
                "            border-radius: 50%;\n" +
                "            background: radial-gradient(circle, rgba(255,255,255,0.8), transparent);\n" +
                "            animation: collisionPulse 0.5s ease-out;\n" +
                "            pointer-events: none;\n" +
                "            z-index: 20;\n" +
                "        }\n" +
                "        @keyframes collisionPulse {\n" +
                "            0% { transform: scale(0); opacity: 1; }\n" +
                "            100% { transform: scale(2); opacity: 0; }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">\n" +
                "            <h1>🎮 Juego de Héroes 2D - Batalla Épica</h1>\n" +
                "            <p>🔥 Personajes 2D • ⚔️ Combate Aleatorio • 🌟 Habilidades Especiales</p>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"instructions\">\n" +
                "            <h3>🎯 Cómo Jugar:</h3>\n" +
                "            <p>• <strong>WASD/Flechas</strong> - Mover héroe rojo</p>\n" +
                "            <p>• <strong>Espacio</strong> - Atacar enemigo cercano</p>\n" +
                "            <p>• <strong>Click</strong> - Equipar ítems y usar habilidades</p>\n" +
                "            <p>• <strong>Colisiona</strong> con enemigos para combate aleatorio</p>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"game-container\">\n" +
                "            <div class=\"panel\">\n" +
                "                <h3>📊 Estadísticas del Héroe</h3>\n" +
                "                <div class=\"stats-display\">\n" +
                "                    <div class=\"stat-item\">\n" +
                "                        <span class=\"stat-label\">❤️ Vida</span>\n" +
                "                        <div class=\"stat-bar\">\n" +
                "                            <div class=\"stat-fill vida-fill\" id=\"vidaBar\"></div>\n" +
                "                        </div>\n" +
                "                        <span class=\"stat-value\" id=\"vidaValue\">3/5</span>\n" +
                "                    </div>\n" +
                "                    <div class=\"stat-item\">\n" +
                "                        <span class=\"stat-label\">🛡️ Escudo</span>\n" +
                "                        <div class=\"stat-bar\">\n" +
                "                            <div class=\"stat-fill escudo-fill\" id=\"escudoBar\"></div>\n" +
                "                        </div>\n" +
                "                        <span class=\"stat-value\" id=\"escudoValue\">1/5</span>\n" +
                "                    </div>\n" +
                "                    <div class=\"stat-item\">\n" +
                "                        <span class=\"stat-label\">⚔️ Ataque</span>\n" +
                "                        <div class=\"stat-bar\">\n" +
                "                            <div class=\"stat-fill ataque-fill\" id=\"ataqueBar\"></div>\n" +
                "                        </div>\n" +
                "                        <span class=\"stat-value\" id=\"ataqueValue\">2/5</span>\n" +
                "                    </div>\n" +
                "                    <div class=\"stat-item\">\n" +
                "                        <span class=\"stat-label\">💥 Crítico</span>\n" +
                "                        <div class=\"stat-bar\">\n" +
                "                            <div class=\"stat-fill critico-fill\" id=\"criticoBar\"></div>\n" +
                "                        </div>\n" +
                "                        <span class=\"stat-value\" id=\"criticoValue\">2/5</span>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "                <div class=\"equipment-display\">\n" +
                "                    <strong>🎒 Equipamiento:</strong>\n" +
                "                    <div id=\"equipmentList\">Ninguno</div>\n" +
                "                </div>\n" +
                "                <div class=\"enemy-info\">\n" +
                "                    <h4>👹 Enemigo Actual</h4>\n" +
                "                    <div id=\"enemyInfo\">No hay enemigo</div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "\n" +
                "            <div class=\"panel\">\n" +
                "                <h3>🎮 Arena de Batalla</h3>\n" +
                "                <div class=\"game-area\" id=\"gameArea\">\n" +
                "                    <div class=\"hero\" id=\"hero\"></div>\n" +
                "                </div>\n" +
                "                <div id=\"messageArea\"></div>\n" +
                "            </div>\n" +
                "\n" +
                "            <div class=\"panel\">\n" +
                "                <h3>⚔️ Controles de Combate</h3>\n" +
                "                <div class=\"controls\">\n" +
                "                    <button class=\"btn btn-equipar\" onclick=\"equipar('espada')\">⚔️ Espada</button>\n" +
                "                    <button class=\"btn btn-equipar\" onclick=\"equipar('escudo')\">🛡️ Escudo</button>\n" +
                "                    <button class=\"btn btn-equipar\" onclick=\"equipar('poder')\">💪 Poder</button>\n" +
                "                    <button class=\"btn btn-equipar\" onclick=\"equipar('buffataque')\">🔥 Fuego</button>\n" +
                "                    <button class=\"btn btn-equipar\" onclick=\"equipar('buffmultiplicador')\">⚡ Rayo</button>\n" +
                "                    <button class=\"btn btn-especial\" onclick=\"spawnEnemy()\">👹 Generar</button>\n" +
                "                    <button class=\"btn btn-accion\" onclick=\"atacar()\">⚔️ Atacar</button>\n" +
                "                    <button class=\"btn btn-especial\" onclick=\"specialAttack()\">🌟 Especial</button>\n" +
                "                    <button class=\"btn btn-reset\" onclick=\"reset()\">🔄 Reset</button>\n" +
                "                </div>\n" +
                "                <div class=\"combat-log\">\n" +
                "                    <h4>⚔️ Registro de Combate</h4>\n" +
                "                    <div id=\"combatLog\">Esperando acción...</div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        let heroX = 280;\n" +
                "        let heroY = 180;\n" +
                "        let enemies = [];\n" +
                "        const moveSpeed = 15;\n" +
                "        const gameAreaSize = { width: 600, height: 400 };\n" +
                "        const heroSize = 40;\n" +
                "        const enemySize = 35;\n" +
                "        let currentEnemy = null;\n" +
                "\n" +
                "        // Initialize game\n" +
                "        document.addEventListener('DOMContentLoaded', function() {\n" +
                "            updateStats();\n" +
                "            updateHeroPosition();\n" +
                "            setupControls();\n" +
                "            startGameLoop();\n" +
                "        });\n" +
                "\n" +
                "        function setupControls() {\n" +
                "            document.addEventListener('keydown', function(event) {\n" +
                "                let dx = 0, dy = 0;\n" +
                "                \n" +
                "                switch(event.key.toLowerCase()) {\n" +
                "                    case 'w':\n" +
                "                    case 'arrowup':\n" +
                "                        dy = -moveSpeed;\n" +
                "                        break;\n" +
                "                    case 's':\n" +
                "                    case 'arrowdown':\n" +
                "                        dy = moveSpeed;\n" +
                "                        break;\n" +
                "                    case 'a':\n" +
                "                    case 'arrowleft':\n" +
                "                        dx = -moveSpeed;\n" +
                "                        break;\n" +
                "                    case 'd':\n" +
                "                    case 'arrowright':\n" +
                "                        dx = moveSpeed;\n" +
                "                        break;\n" +
                "                    case ' ':\n" +
                "                        event.preventDefault();\n" +
                "                        checkAndAttack();\n" +
                "                        return;\n" +
                "                    default:\n" +
                "                        return;\n" +
                "                }\n" +
                "\n" +
                "                event.preventDefault();\n" +
                "                moveHero(dx, dy);\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function moveHero(dx, dy) {\n" +
                "            const newX = Math.max(0, Math.min(heroX + dx, gameAreaSize.width - heroSize));\n" +
                "            const newY = Math.max(0, Math.min(heroY + dy, gameAreaSize.height - heroSize));\n" +
                "            \n" +
                "            fetch('/mover', {\n" +
                "                method: 'POST',\n" +
                "                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },\n" +
                "                body: `dx=${dx}&dy=${dy}`\n" +
                "            })\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                if (data.message) {\n" +
                "                    heroX = newX;\n" +
                "                    heroY = newY;\n" +
                "                    updateHeroPosition();\n" +
                "                    updateStatsDisplay(data.stats);\n" +
                "                    checkCollisions();\n" +
                "                } else if (data.error) {\n" +
                "                    showMessage(data.error, 'error');\n" +
                "                }\n" +
                "            })\n" +
                "            .catch(error => {\n" +
                "                showMessage('Error al mover: ' + error, 'error');\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function updateHeroPosition() {\n" +
                "            const hero = document.getElementById('hero');\n" +
                "            hero.style.left = heroX + 'px';\n" +
                "            hero.style.top = heroY + 'px';\n" +
                "        }\n" +
                "\n" +
                "        function spawnEnemy() {\n" +
                "            fetch('/spawn-enemy', {\n" +
                "                method: 'POST',\n" +
                "                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }\n" +
                "            })\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                if (data.message) {\n" +
                "                    showMessage(data.message, 'info');\n" +
                "                    addEnemyToArena(data.enemigo);\n" +
                "                } else if (data.error) {\n" +
                "                    showMessage(data.error, 'error');\n" +
                "                }\n" +
                "            })\n" +
                "            .catch(error => {\n" +
                "                showMessage('Error al generar enemigo: ' + error, 'error');\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function addEnemyToArena(enemyData) {\n" +
                "            const gameArea = document.getElementById('gameArea');\n" +
                "            \n" +
                "            // Remove existing enemy if any\n" +
                "            const existingEnemy = document.querySelector('.enemy');\n" +
                "            if (existingEnemy) {\n" +
                "                existingEnemy.remove();\n" +
                "            }\n" +
                "            \n" +
                "            // Create new enemy at random position\n" +
                "            const enemy = document.createElement('div');\n" +
                "            enemy.className = 'enemy';\n" +
                "            enemy.id = 'enemy';\n" +
                "            \n" +
                "            const enemyX = Math.random() * (gameAreaSize.width - enemySize);\n" +
                "            const enemyY = Math.random() * (gameAreaSize.height - enemySize);\n" +
                "            \n" +
                "            enemy.style.left = enemyX + 'px';\n" +
                "            enemy.style.top = enemyY + 'px';\n" +
                "            \n" +
                "            gameArea.appendChild(enemy);\n" +
                "            \n" +
                "            currentEnemy = {\n" +
                "                element: enemy,\n" +
                "                x: enemyX,\n" +
                "                y: enemyY,\n" +
                "                data: enemyData\n" +
                "            };\n" +
                "            \n" +
                "            updateEnemyInfo(enemyData);\n" +
                "        }\n" +
                "\n" +
                "        function updateEnemyInfo(enemyData) {\n" +
                "            const enemyInfo = document.getElementById('enemyInfo');\n" +
                "            enemyInfo.innerHTML = `\n" +
                "                <strong>${enemyData.nombre}</strong><br>\n" +
                "                ❤️ ${enemyData.vida}/${enemyData.maxVida}<br>\n" +
                "                ⚔️ ${enemyData.ataque} 🛡️ ${enemyData.escudo}<br>\n" +
                "                💥 ${enemyData.critico}% crítico\n" +
                "            `;\n" +
                "        }\n" +
                "\n" +
                "        function checkCollisions() {\n" +
                "            if (!currentEnemy) return;\n" +
                "            \n" +
                "            const distance = Math.sqrt(\n" +
                "                Math.pow(heroX - currentEnemy.x, 2) + \n" +
                "                Math.pow(heroY - currentEnemy.y, 2)\n" +
                "            );\n" +
                "            \n" +
                "            if (distance < (heroSize + enemySize) / 2) {\n" +
                "                // Collision detected!\n" +
                "                showCollisionEffect(heroX + heroSize/2, heroY + heroSize/2);\n" +
                "                performRandomCombat();\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function showCollisionEffect(x, y) {\n" +
                "            const gameArea = document.getElementById('gameArea');\n" +
                "            const effect = document.createElement('div');\n" +
                "            effect.className = 'collision-effect';\n" +
                "            effect.style.left = (x - 30) + 'px';\n" +
                "            effect.style.top = (y - 30) + 'px';\n" +
                "            \n" +
                "            gameArea.appendChild(effect);\n" +
                "            \n" +
                "            setTimeout(() => {\n" +
                "                effect.remove();\n" +
                "            }, 500);\n" +
                "        }\n" +
                "\n" +
                "        function performRandomCombat() {\n" +
                "            if (!currentEnemy) return;\n" +
                "            \n" +
                "            fetch('/atacar', {\n" +
                "                method: 'POST',\n" +
                "                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }\n" +
                "            })\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                if (data.ganador) {\n" +
                "                    displayCombatResults(data);\n" +
                "                    updateStatsDisplay(data.heroStats);\n" +
                "                    \n" +
                "                    // Remove enemy if hero won\n" +
                "                    if (data.ganador === 'HeroeBase') {\n" +
                "                        removeEnemy();\n" +
                "                    }\n" +
                "                } else if (data.error) {\n" +
                "                    showMessage(data.error, 'error');\n" +
                "                }\n" +
                "            })\n" +
                "            .catch(error => {\n" +
                "                showMessage('Error en combate: ' + error, 'error');\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function displayCombatResults(data) {\n" +
                "            const combatLog = document.getElementById('combatLog');\n" +
                "            \n" +
                "            // Add victory/defeat message\n" +
                "            const resultEntry = document.createElement('div');\n" +
                "            resultEntry.className = 'combat-entry ' + (data.ganador === 'HeroeBase' ? 'victory' : 'enemy');\n" +
                "            resultEntry.innerHTML = `<strong>${data.ganador === 'HeroeBase' ? '🎉 ¡VICTORIA!' : '💀 DERROTA'}</strong> contra ${data.enemigo.nombre}`;\n" +
                "            combatLog.insertBefore(resultEntry, combatLog.firstChild);\n" +
                "            \n" +
                "            // Add detailed combat log\n" +
                "            if (data.detallesLog && data.detallesLog.length > 0) {\n" +
                "                data.detallesLog.forEach(entry => {\n" +
                "                    const logEntry = document.createElement('div');\n" +
                "                    logEntry.className = 'combat-entry';\n" +
                "                    \n" +
                "                    if (entry.includes('Crítico')) {\n" +
                "                        logEntry.className += ' critical';\n" +
                "                    } else if (entry.includes('contraatac')) {\n" +
                "                        logEntry.className += ' enemy';\n" +
                "                    }\n" +
                "                    \n" +
                "                    logEntry.textContent = entry;\n" +
                "                    combatLog.insertBefore(logEntry, combatLog.firstChild);\n" +
                "                });\n" +
                "            }\n" +
                "            \n" +
                "            showMessage(`¡${data.ganador} ganó en ${data.turnos} turnos!`, data.ganador === 'HeroeBase' ? 'success' : 'error');\n" +
                "        }\n" +
                "\n" +
                "        function removeEnemy() {\n" +
                "            if (currentEnemy && currentEnemy.element) {\n" +
                "                currentEnemy.element.remove();\n" +
                "                currentEnemy = null;\n" +
                "                document.getElementById('enemyInfo').innerHTML = 'No hay enemigo';\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function checkAndAttack() {\n" +
                "            if (currentEnemy) {\n" +
                "                const distance = Math.sqrt(\n" +
                "                    Math.pow(heroX - currentEnemy.x, 2) + \n" +
                "                    Math.pow(heroY - currentEnemy.y, 2)\n" +
                "                );\n" +
                "                \n" +
                "                if (distance < 100) {\n" +
                "                    performRandomCombat();\n" +
                "                } else {\n" +
                "                    showMessage('¡Acércate al enemigo para atacar!', 'warning');\n" +
                "                }\n" +
                "            } else {\n" +
                "                showMessage('¡Genera un enemigo primero!', 'warning');\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function specialAttack() {\n" +
                "            // Random special ability\n" +
                "            const abilities = ['Fuego Sagrado', 'Rayo Divino', 'Hielo Arcano', 'Tormenta Sombra'];\n" +
                "            const ability = abilities[Math.floor(Math.random() * abilities.length)];\n" +
                "            \n" +
                "            showMessage(`🌟 ${ability} activado!`, 'info');\n" +
                "            \n" +
                "            // Temporary boost\n" +
                "            equipar('buffataque');\n" +
                "            \n" +
                "            if (currentEnemy) {\n" +
                "                setTimeout(() => performRandomCombat(), 500);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function updateStats() {\n" +
                "            fetch('/status')\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                updateStatsDisplay(data);\n" +
                "            })\n" +
                "            .catch(error => {\n" +
                "                showMessage('Error al actualizar estadísticas: ' + error, 'error');\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function updateStatsDisplay(stats) {\n" +
                "            const maxStat = 5;\n" +
                "            \n" +
                "            updateBar('vida', stats.vida, maxStat);\n" +
                "            updateBar('escudo', stats.escudo, maxStat);\n" +
                "            updateBar('ataque', stats.ataque, maxStat);\n" +
                "            updateBar('critico', stats.critico, maxStat);\n" +
                "            \n" +
                "            // Update equipment\n" +
                "            const equipmentList = document.getElementById('equipmentList');\n" +
                "            if (stats.decoradores && stats.decoradores.length > 0) {\n" +
                "                equipmentList.innerHTML = stats.decoradores.map(d => \n" +
                "                    `<span class=\"equipment-item\">${d}</span>`\n" +
                "                ).join('');\n" +
                "            } else {\n" +
                "                equipmentList.innerHTML = '<span class=\"equipment-item\">Ninguno</span>';\n" +
                "            }\n" +
                "            \n" +
                "            // Update hero visual based on swords\n" +
                "            updateHeroVisual(stats);\n" +
                "        }\n" +
                "\n" +
                "        function updateHeroVisual(stats) {\n" +
                "            const hero = document.getElementById('hero');\n" +
                "            hero.classList.remove('sword-1', 'sword-2', 'sword-3');\n" +
                "            \n" +
                "            let swordCount = 0;\n" +
                "            if (stats.decoradores) {\n" +
                "                swordCount = stats.decoradores.filter(d => d === 'Espada').length;\n" +
                "            }\n" +
                "            \n" +
                "            if (swordCount >= 1) {\n" +
                "                hero.classList.add('sword-' + swordCount);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function updateBar(statName, value, maxValue) {\n" +
                "            const bar = document.getElementById(statName + 'Bar');\n" +
                "            const valueSpan = document.getElementById(statName + 'Value');\n" +
                "            const percentage = (value / maxValue) * 100;\n" +
                "            \n" +
                "            bar.style.width = percentage + '%';\n" +
                "            valueSpan.textContent = value + '/' + maxValue;\n" +
                "        }\n" +
                "\n" +
                "        function equipar(item) {\n" +
                "            fetch('/equipar', {\n" +
                "                method: 'POST',\n" +
                "                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },\n" +
                "                body: `item=${item}`\n" +
                "            })\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                if (data.message) {\n" +
                "                    showMessage(data.message, 'success');\n" +
                "                    updateStatsDisplay(data.stats);\n" +
                "                } else if (data.error) {\n" +
                "                    showMessage(data.error, 'error');\n" +
                "                }\n" +
                "            })\n" +
                "            .catch(error => {\n" +
                "                showMessage('Error al equipar: ' + error, 'error');\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function atacar() {\n" +
                "            checkAndAttack();\n" +
                "        }\n" +
                "\n" +
                "        function reset() {\n" +
                "            if (confirm('¿Estás seguro de que quieres resetear el juego?')) {\n" +
                "                fetch('/reset', {\n" +
                "                    method: 'POST',\n" +
                "                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }\n" +
                "                })\n" +
                "                .then(response => response.json())\n" +
                "                .then(data => {\n" +
                "                    if (data.message) {\n" +
                "                        showMessage(data.message, 'success');\n" +
                "                        updateStatsDisplay(data.stats);\n" +
                "                        heroX = 280;\n" +
                "                        heroY = 180;\n" +
                "                        updateHeroPosition();\n" +
                "                        removeEnemy();\n" +
                "                        document.getElementById('combatLog').innerHTML = 'Esperando acción...';\n" +
                "                    } else if (data.error) {\n" +
                "                        showMessage(data.error, 'error');\n" +
                "                    }\n" +
                "                })\n" +
                "                .catch(error => {\n" +
                "                    showMessage('Error al resetear: ' + error, 'error');\n" +
                "                });\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function showMessage(message, type) {\n" +
                "            const messageArea = document.getElementById('messageArea');\n" +
                "            const messageDiv = document.createElement('div');\n" +
                "            messageDiv.className = `message ${type}`;\n" +
                "            messageDiv.textContent = message;\n" +
                "            \n" +
                "            messageArea.innerHTML = '';\n" +
                "            messageArea.appendChild(messageDiv);\n" +
                "            \n" +
                "            setTimeout(() => {\n" +
                "                messageDiv.style.opacity = '0';\n" +
                "                setTimeout(() => {\n" +
                "                    if (messageDiv.parentNode) {\n" +
                "                        messageDiv.parentNode.removeChild(messageDiv);\n" +
                "                    }\n" +
                "                }, 300);\n" +
                "            }, 3000);\n" +
                "        }\n" +
                "\n" +
                "        function startGameLoop() {\n" +
                "            setInterval(() => {\n" +
                "                checkCollisions();\n" +
                "            }, 100);\n" +
                "            \n" +
                "            setInterval(updateStats, 5000);\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
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
}
