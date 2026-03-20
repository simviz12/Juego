import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class JuegoServidor {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // GET /juego endpoint - serve HTML game interface
        server.createContext("/juego", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!exchange.getRequestMethod().equals("GET")) {
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    return;
                }
                
                String htmlContent = getGameHTML();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, htmlContent.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(htmlContent.getBytes());
                os.close();
            }
        });
        
        // Hello World endpoint
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "¡Hola Mundo desde el servidor Java del Juego de Héroes!";
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        });
        
        // GET /status endpoint
        server.createContext("/status", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!exchange.getRequestMethod().equals("GET")) {
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    return;
                }
                
                GameState gameState = GameState.getInstance();
                String jsonResponse = gameState.getHeroeStatsJson();
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
            }
        });
        
        // POST /equipar endpoint
        server.createContext("/equipar", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!exchange.getRequestMethod().equals("POST")) {
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    return;
                }
                
                // Read request body
                InputStream is = exchange.getRequestBody();
                String requestBody = new String(is.readAllBytes());
                is.close();
                
                try {
                    Map<String, String> params = parseQueryParams(requestBody);
                    String item = params.get("item");
                    
                    if (item == null || item.isEmpty()) {
                        sendErrorResponse(exchange, 400, "Missing 'item' parameter");
                        return;
                    }
                    
                    GameState gameState = GameState.getInstance();
                    Personaje heroeActual = gameState.getHeroe();
                    
                    try {
                        switch (item.toLowerCase()) {
                            case "espada":
                                heroeActual = new Espada(heroeActual);
                                break;
                            case "escudo":
                                heroeActual = new Escudo(heroeActual);
                                break;
                            case "poder":
                                heroeActual = new Poder(heroeActual);
                                break;
                            case "buffataque":
                                heroeActual = new BuffAtaque(heroeActual, 2, 3);
                                break;
                            case "buffmultiplicador":
                                heroeActual = new BuffMultiplicador(heroeActual, 1.5, 2);
                                break;
                            default:
                                sendErrorResponse(exchange, 400, "Unknown item: " + item);
                                return;
                        }
                        
                        gameState.setHeroe(heroeActual);
                        
                        String response = "{\"message\":\"Item equipped successfully\",\"stats\":" + 
                                        gameState.getHeroeStatsJson() + "}";
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        
                    } catch (TooManySwordsException e) {
                        sendErrorResponse(exchange, 400, e.getMessage());
                    }
                    
                } catch (Exception e) {
                    sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
                }
            }
        });
        
        // POST /atacar endpoint
        server.createContext("/atacar", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!exchange.getRequestMethod().equals("POST")) {
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    return;
                }
                
                try {
                    GameState gameState = GameState.getInstance();
                    Personaje heroe = gameState.getHeroe();
                    Enemigo enemigo = gameState.getEnemigoActual();
                    
                    // Create a new enemy for each attack
                    String[] nombresEnemigos = {"Goblin", "Orco", "Esqueleto", "Duende", "Ladrón"};
                    String nombre = nombresEnemigos[(int)(Math.random() * nombresEnemigos.length)];
                    int vida = 2 + (int)(Math.random() * 3); // 2-4
                    int critico = 1 + (int)(Math.random() * 2); // 1-2
                    int escudo = 1 + (int)(Math.random() * 2); // 1-2
                    int ataque = 2 + (int)(Math.random() * 2); // 2-3
                    
                    enemigo = new Enemigo(nombre, vida, critico, escudo, ataque);
                    gameState.setEnemigoActual(enemigo);
                    
                    MotorCombate.ResultadoCombate resultado = MotorCombate.simularCombate(heroe, enemigo);
                    
                    StringBuilder jsonResponse = new StringBuilder();
                    jsonResponse.append("{");
                    jsonResponse.append("\"ganador\":\"").append(resultado.ganador).append("\",");
                    jsonResponse.append("\"turnos\":").append(resultado.turnos).append(",");
                    jsonResponse.append("\"detallesLog\":[");
                    
                    // Add detailed combat log
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
                    sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
                }
            }
        });
        
        // POST /mover endpoint
        server.createContext("/mover", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!exchange.getRequestMethod().equals("POST")) {
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    return;
                }
                
                // Read request body
                InputStream is = exchange.getRequestBody();
                String requestBody = new String(is.readAllBytes());
                is.close();
                
                try {
                    Map<String, String> params = parseQueryParams(requestBody);
                    String dxStr = params.get("dx");
                    String dyStr = params.get("dy");
                    
                    if (dxStr == null || dyStr == null) {
                        sendErrorResponse(exchange, 400, "Missing 'dx' or 'dy' parameters");
                        return;
                    }
                    
                    int dx = Integer.parseInt(dxStr);
                    int dy = Integer.parseInt(dyStr);
                    
                    GameState gameState = GameState.getInstance();
                    gameState.moveHero(dx, dy);
                    
                    String response = "{\"message\":\"Hero moved successfully\",\"stats\":" + 
                                    gameState.getHeroeStatsJson() + "}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid dx/dy values");
                } catch (Exception e) {
                    sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
                }
            }
        });
        
        // POST /reset endpoint
        server.createContext("/reset", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!exchange.getRequestMethod().equals("POST")) {
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    return;
                }
                
                GameState gameState = GameState.getInstance();
                gameState.resetHeroe();
                
                String response = "{\"message\":\"Hero reset successfully\",\"stats\":" + 
                                gameState.getHeroeStatsJson() + "}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        });
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("Servidor del Juego de Héroes iniciado en http://localhost:8080");
        System.out.println("Endpoints disponibles:");
        System.out.println("  GET  /           - Hello World");
        System.out.println("  GET  /juego      - Interfaz del juego HTML");
        System.out.println("  GET  /status     - Ver estadísticas del héroe (JSON)");
        System.out.println("  POST /equipar    - Equipar un ítem (item=espada|escudo|poder|buffataque|buffmultiplicador)");
        System.out.println("  POST /atacar     - Atacar a un enemigo");
        System.out.println("  POST /mover      - Mover héroe (dx,dy)");
        System.out.println("  POST /reset      - Resetear el héroe");
        System.out.println("Presiona Ctrl+C para detener el servidor");
    }
    
    private static String getGameHTML() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Juego de Héroes</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            margin: 0;\n" +
                "            padding: 20px;\n" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
                "            color: white;\n" +
                "            min-height: 100vh;\n" +
                "        }\n" +
                "        .container {\n" +
                "            max-width: 1200px;\n" +
                "            margin: 0 auto;\n" +
                "        }\n" +
                "        .game-area {\n" +
                "            width: 500px;\n" +
                "            height: 500px;\n" +
                "            border: 3px solid #fff;\n" +
                "            margin: 20px auto;\n" +
                "            position: relative;\n" +
                "            background: rgba(0,0,0,0.3);\n" +
                "            border-radius: 10px;\n" +
                "        }\n" +
                "        .hero {\n" +
                "            width: 30px;\n" +
                "            height: 30px;\n" +
                "            background: #ff6b6b;\n" +
                "            border: 2px solid #fff;\n" +
                "            border-radius: 50%;\n" +
                "            position: absolute;\n" +
                "            transition: all 0.2s ease;\n" +
                "            z-index: 10;\n" +
                "        }\n" +
                "        .hero.sword-1 {\n" +
                "            box-shadow: 0 0 10px rgba(255, 107, 107, 0.6);\n" +
                "            background: linear-gradient(45deg, #ff6b6b, #ff8e8e);\n" +
                "        }\n" +
                "        .hero.sword-2 {\n" +
                "            box-shadow: 0 0 20px rgba(255, 107, 107, 0.8), 0 0 30px rgba(255, 215, 0, 0.4);\n" +
                "            background: linear-gradient(45deg, #ff6b6b, #ffd700);\n" +
                "            animation: glow-2 2s ease-in-out infinite alternate;\n" +
                "        }\n" +
                "        .hero.sword-3 {\n" +
                "            box-shadow: 0 0 30px rgba(255, 107, 107, 1), 0 0 40px rgba(255, 215, 0, 0.8), 0 0 50px rgba(255, 0, 0, 0.4);\n" +
                "            background: linear-gradient(45deg, #ff6b6b, #ff0000);\n" +
                "            animation: glow-3 1s ease-in-out infinite alternate;\n" +
                "        }\n" +
                "        @keyframes glow-2 {\n" +
                "            from { transform: scale(1); }\n" +
                "            to { transform: scale(1.1); }\n" +
                "        }\n" +
                "        @keyframes glow-3 {\n" +
                "            from { transform: scale(1); filter: brightness(1); }\n" +
                "            to { transform: scale(1.2); filter: brightness(1.3); }\n" +
                "        }\n" +
                "        .onion-display {\n" +
                "            background: rgba(255,255,255,0.1);\n" +
                "            padding: 20px;\n" +
                "            border-radius: 15px;\n" +
                "            margin: 20px 0;\n" +
                "            backdrop-filter: blur(10px);\n" +
                "        }\n" +
                "        .onion-layers {\n" +
                "            display: flex;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            gap: 10px;\n" +
                "            margin: 15px 0;\n" +
                "        }\n" +
                "        .layer {\n" +
                "            padding: 8px 15px;\n" +
                "            border-radius: 20px;\n" +
                "            font-size: 12px;\n" +
                "            font-weight: bold;\n" +
                "            text-transform: uppercase;\n" +
                "            border: 2px solid rgba(255,255,255,0.3);\n" +
                "            transition: all 0.3s ease;\n" +
                "        }\n" +
                "        .layer-base {\n" +
                "            background: rgba(255,255,255,0.2);\n" +
                "        }\n" +
                "        .layer-espada {\n" +
                "            background: rgba(255,107,107,0.6);\n" +
                "            border-color: #ff6b6b;\n" +
                "        }\n" +
                "        .layer-escudo {\n" +
                "            background: rgba(78,205,196,0.6);\n" +
                "            border-color: #4ecdc4;\n" +
                "        }\n" +
                "        .layer-poder {\n" +
                "            background: rgba(255,230,109,0.6);\n" +
                "            border-color: #ffe66d;\n" +
                "        }\n" +
                "        .layer-buff {\n" +
                "            background: rgba(168,230,207,0.6);\n" +
                "            border-color: #a8e6cf;\n" +
                "            animation: pulse 2s ease-in-out infinite;\n" +
                "        }\n" +
                "        @keyframes pulse {\n" +
                "            0%, 100% { transform: scale(1); opacity: 0.8; }\n" +
                "            50% { transform: scale(1.05); opacity: 1; }\n" +
                "        }\n" +
                "        .detailed-combat-log {\n" +
                "            background: rgba(0,0,0,0.4);\n" +
                "            padding: 20px;\n" +
                "            border-radius: 15px;\n" +
                "            margin: 20px 0;\n" +
                "            max-height: 300px;\n" +
                "            overflow-y: auto;\n" +
                "            border: 2px solid rgba(255,255,255,0.1);\n" +
                "        }\n" +
                "        .combat-entry {\n" +
                "            padding: 8px 12px;\n" +
                "            margin: 5px 0;\n" +
                "            border-radius: 8px;\n" +
                "            border-left: 4px solid #4ecdc4;\n" +
                "            background: rgba(255,255,255,0.05);\n" +
                "            animation: slideIn 0.3s ease-out;\n" +
                "        }\n" +
                "        .combat-entry.critical {\n" +
                "            border-left-color: #ff6b6b;\n" +
                "            background: rgba(255,107,107,0.1);\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .combat-entry.enemy {\n" +
                "            border-left-color: #ffe66d;\n" +
                "            background: rgba(255,230,109,0.1);\n" +
                "        }\n" +
                "        @keyframes slideIn {\n" +
                "            from { transform: translateX(-20px); opacity: 0; }\n" +
                "            to { transform: translateX(0); opacity: 1; }\n" +
                "        }\n" +
                "        .stress-test {\n" +
                "            background: rgba(255,0,0,0.1);\n" +
                "            border: 2px solid rgba(255,0,0,0.3);\n" +
                "            padding: 15px;\n" +
                "            border-radius: 10px;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        .stress-btn {\n" +
                "            background: linear-gradient(45deg, #ff6b6b, #ff0000);\n" +
                "            color: white;\n" +
                "            padding: 15px 30px;\n" +
                "            border: none;\n" +
                "            border-radius: 8px;\n" +
                "            font-weight: bold;\n" +
                "            cursor: pointer;\n" +
                "            transition: all 0.3s ease;\n" +
                "            text-transform: uppercase;\n" +
                "        }\n" +
                "        .stress-btn:hover {\n" +
                "            transform: translateY(-2px);\n" +
                "            box-shadow: 0 5px 15px rgba(255,0,0,0.3);\n" +
                "        }\n" +
                "        .stress-btn:active {\n" +
                "            transform: translateY(0);\n" +
                "        }\n" +
                "        .code-quality {\n" +
                "            background: rgba(0,255,0,0.1);\n" +
                "            border: 2px solid rgba(0,255,0,0.3);\n" +
                "            padding: 15px;\n" +
                "            border-radius: 10px;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        .stats-panel {\n" +
                "            background: rgba(255,255,255,0.1);\n" +
                "            padding: 20px;\n" +
                "            border-radius: 10px;\n" +
                "            margin: 20px 0;\n" +
                "            backdrop-filter: blur(10px);\n" +
                "        }\n" +
                "        .stat-bar {\n" +
                "            margin: 10px 0;\n" +
                "        }\n" +
                "        .stat-label {\n" +
                "            display: inline-block;\n" +
                "            width: 80px;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .bar-container {\n" +
                "            display: inline-block;\n" +
                "            width: 200px;\n" +
                "            height: 20px;\n" +
                "            background: rgba(255,255,255,0.2);\n" +
                "            border-radius: 10px;\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "        .bar-fill {\n" +
                "            height: 100%;\n" +
                "            transition: width 0.3s ease;\n" +
                "        }\n" +
                "        .vida-fill { background: #ff6b6b; }\n" +
                "        .escudo-fill { background: #4ecdc4; }\n" +
                "        .ataque-fill { background: #ffe66d; }\n" +
                "        .critico-fill { background: #a8e6cf; }\n" +
                "        .controls {\n" +
                "            display: grid;\n" +
                "            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));\n" +
                "            gap: 10px;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        .btn {\n" +
                "            padding: 12px 20px;\n" +
                "            border: none;\n" +
                "            border-radius: 5px;\n" +
                "            cursor: pointer;\n" +
                "            font-weight: bold;\n" +
                "            transition: all 0.3s ease;\n" +
                "            text-transform: uppercase;\n" +
                "        }\n" +
                "        .btn-equipar {\n" +
                "            background: #4ecdc4;\n" +
                "            color: white;\n" +
                "        }\n" +
                "        .btn-equipar:hover {\n" +
                "            background: #45b7b8;\n" +
                "            transform: translateY(-2px);\n" +
                "        }\n" +
                "        .btn-accion {\n" +
                "            background: #ff6b6b;\n" +
                "            color: white;\n" +
                "        }\n" +
                "        .btn-accion:hover {\n" +
                "            background: #ff5252;\n" +
                "            transform: translateY(-2px);\n" +
                "        }\n" +
                "        .btn-reset {\n" +
                "            background: #95a5a6;\n" +
                "            color: white;\n" +
                "        }\n" +
                "        .btn-reset:hover {\n" +
                "            background: #7f8c8d;\n" +
                "        }\n" +
                "        .message {\n" +
                "            padding: 10px;\n" +
                "            margin: 10px 0;\n" +
                "            border-radius: 5px;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .success { background: #a8e6cf; color: #2d3436; }\n" +
                "        .error { background: #ff6b6b; color: white; }\n" +
                "        .info { background: #ffe66d; color: #2d3436; }\n" +
                "        .equipment-list {\n" +
                "            margin: 10px 0;\n" +
                "            padding: 10px;\n" +
                "            background: rgba(255,255,255,0.1);\n" +
                "            border-radius: 5px;\n" +
                "        }\n" +
                "        .instructions {\n" +
                "            background: rgba(255,255,255,0.1);\n" +
                "            padding: 15px;\n" +
                "            border-radius: 10px;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        .combat-log {\n" +
                "            background: rgba(0,0,0,0.3);\n" +
                "            padding: 15px;\n" +
                "            border-radius: 10px;\n" +
                "            margin: 20px 0;\n" +
                "            max-height: 200px;\n" +
                "            overflow-y: auto;\n" +
                "        }\n" +
                "        .container {\n" +
                "            max-width: 1200px;\n" +
                "            margin: 0 auto;\n" +
                "        }\n" +
                "        .game-area {\n" +
                "            width: 500px;\n" +
                "            height: 500px;\n" +
                "            border: 3px solid #fff;\n" +
                "            margin: 20px auto;\n" +
                "            position: relative;\n" +
                "            background: rgba(0,0,0,0.3);\n" +
                "            border-radius: 10px;\n" +
                "        }\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            margin: 0;\n" +
                "            padding: 20px;\n" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
                "            color: white;\n" +
                "            min-height: 100vh;\n" +
                "        }"\n" +
                "\n" +
                "    <script>\n" +
                "        let heroX = 250;\n" +
                "        let heroY = 250;\n" +
                "        const moveSpeed = 10;\n" +
                "        const gameAreaSize = 500;\n" +
                "        const heroSize = 30;\n" +
                "\n" +
                "        // Initialize game\n" +
                "        document.addEventListener('DOMContentLoaded', function() {\n" +
                "            updateStats();\n" +
                "            updateHeroPosition();\n" +
                "            setupKeyboardControls();\n" +
                "        });\n" +
                "\n" +
                "        function setupKeyboardControls() {\n" +
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
                "                    default:\n" +
                "                        return;\n" +
                "                }\n" +
                "\n" +
                "                event.preventDefault();\n" +
                "                moveHero(dx, dy);\n" +
                "            });\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <h1>🎮 Juego de Héroes</h1>\n" +
                "        \n" +
                "        <div class=\"instructions\">\n" +
                "            <h3>📋 Instrucciones:</h3>\n" +
                "            <p>• Usa <strong>WASD</strong> o <strong>Flechas</strong> para mover el héroe</p>\n" +
                "            <p>• Equipa ítems para mejorar tus estadísticas</p>\n" +
                "            <p>• Ataca enemigos para ganar experiencia</p>\n" +
                "            <p>• Máximo 3 espadas permitidas</p>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"game-area\" id=\"gameArea\">\n" +
                "            <div class=\"hero\" id=\"hero\"></div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"stats-panel\">\n" +
                "            <h2>📊 Estadísticas del Héroe</h2>\n" +
                "            <div class=\"stat-bar\">\n" +
                "                <span class=\"stat-label\">❤️ Vida:</span>\n" +
                "                <div class=\"bar-container\">\n" +
                "                    <div class=\"bar-fill vida-fill\" id=\"vidaBar\"></div>\n" +
                "                </div>\n" +
                "                <span id=\"vidaValue\">3/5</span>\n" +
                "            </div>\n" +
                "            <div class=\"stat-bar\">\n" +
                "                <span class=\"stat-label\">🛡️ Escudo:</span>\n" +
                "                <div class=\"bar-container\">\n" +
                "                    <div class=\"bar-fill escudo-fill\" id=\"escudoBar\"></div>\n" +
                "                </div>\n" +
                "                <span id=\"escudoValue\">1/5</span>\n" +
                "            </div>\n" +
                "            <div class=\"stat-bar\">\n" +
                "                <span class=\"stat-label\">⚔️ Ataque:</span>\n" +
                "                <div class=\"bar-container\">\n" +
                "                    <div class=\"bar-fill ataque-fill\" id=\"ataqueBar\"></div>\n" +
                "                </div>\n" +
                "                <span id=\"ataqueValue\">2/5</span>\n" +
                "            </div>\n" +
                "            <div class=\"stat-bar\">\n" +
                "                <span class=\"stat-label\">💥 Crítico:</span>\n" +
                "                <div class=\"bar-container\">\n" +
                "                    <div class=\"bar-fill critico-fill\" id=\"criticoBar\"></div>\n" +
                "                </div>\n" +
                "                <span id=\"criticoValue\">2/5</span>\n" +
                "            </div>\n" +
                "            <div class=\"equipment-list\">\n" +
                "                <strong>🎒 Equipamiento:</strong>\n" +
                "                <div id=\"equipmentList\">Ninguno</div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"onion-display\">\n" +
                "            <h3>🧅 Visualización de la Cebolla (Decoradores)</h3>\n" +
                "            <div class=\"onion-layers\" id=\"onionLayers\">\n" +
                "                <div class=\"layer layer-base\">HeroeBase</div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"controls\">\n" +
                "            <button class=\"btn btn-equipar\" onclick=\"equipar('espada')\">⚔️ Espada</button>\n" +
                "            <button class=\"btn btn-equipar\" onclick=\"equipar('escudo')\">🛡️ Escudo</button>\n" +
                "            <button class=\"btn btn-equipar\" onclick=\"equipar('poder')\">💪 Poder</button>\n" +
                "            <button class=\"btn btn-equipar\" onclick=\"equipar('buffataque')\">🔥 Buff Ataque</button>\n" +
                "            <button class=\"btn btn-equipar\" onclick=\"equipar('buffmultiplicador')\">⚡ Buff x2</button>\n" +
                "            <button class=\"btn btn-accion\" onclick=\"atacar()\">⚔️ Atacar</button>\n" +
                "            <button class=\"btn btn-reset\" onclick=\"reset()\">🔄 Reset</button>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"stress-test\">\n" +
                "            <h3>🔥 Prueba de Estrés</h3>\n" +
                "            <button class=\"stress-btn\" onclick=\"stressTest()\">💥 EQUIPAR RÁPIDO (10 ítems)</button>\n" +
                "            <p>Intenta romper el sistema equipando muchos ítems rápidamente</p>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"code-quality\">\n" +
                "            <h3>✅ Calidad del Código</h3>\n" +
                "            <p>✨ Variables claras • 📝 Comentarios técnicos • 🏗️ Arquitectura limpia • 🎯 Patrones implementados</p>\n" +
                "        </div>\n" +
                "\n" +
                "        <div id=\"messageArea\"></div>\n" +
                "        \n" +
                "        <div class=\"detailed-combat-log\">\n" +
                "            <h3>⚔️ Log de Combate Detallado</h3>\n" +
                "            <div id=\"detailedLogContent\">Esperando combate...</div>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"combat-log\" id=\"combatLog\">\n" +
                "            <h3>📜 Registro de Combate</h3>\n" +
                "            <div id=\"logContent\">Esperando acciones...</div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        let heroX = 250;\n" +
                "        let heroY = 250;\n" +
                "        const moveSpeed = 10;\n" +
                "        const gameAreaSize = 500;\n" +
                "        const heroSize = 30;\n" +
                "\n" +
                "        // Initialize game\n" +
                "        document.addEventListener('DOMContentLoaded', function() {\n" +
                "            updateStats();\n" +
                "            updateHeroPosition();\n" +
                "            setupKeyboardControls();\n" +
                "        });\n" +
                "\n" +
                "        function setupKeyboardControls() {\n" +
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
                "            fetch('/mover', {\n" +
                "                method: 'POST',\n" +
                "                headers: {\n" +
                "                    'Content-Type': 'application/x-www-form-urlencoded',\n" +
                "                },\n" +
                "                body: `dx=${dx}&dy=${dy}`\n" +
                "            })\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                if (data.message) {\n" +
                "                    updateHeroPosition(data.stats.posicion.x, data.stats.posicion.y);\n" +
                "                    updateStatsDisplay(data.stats);\n" +
                "                } else if (data.error) {\n" +
                "                    showMessage(data.error, 'error');\n" +
                "                }\n" +
                "            })\n" +
                "            .catch(error => {\n" +
                "                showMessage('Error al mover héroe: ' + error, 'error');\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function updateHeroPosition(x, y) {\n" +
                "            if (x !== undefined && y !== undefined) {\n" +
                "                heroX = x;\n" +
                "                heroY = y;\n" +
                "            }\n" +
                "            \n" +
                "            const hero = document.getElementById('hero');\n" +
                "            hero.style.left = heroX + 'px';\n" +
                "            hero.style.top = heroY + 'px';\n" +
                "        }\n" +
                "\n" +
                "        function updateStats() {\n" +
                "            fetch('/status')\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                updateStatsDisplay(data);\n" +
                "                updateHeroPosition(data.posicion.x, data.posicion.y);\n" +
                "            })\n" +
                "            .catch(error => {\n" +
                "                showMessage('Error al actualizar estadísticas: ' + error, 'error');\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function updateStatsDisplay(stats) {\n" +
                "            const maxStat = 5;\n" +
                "            \n" +
                "            // Update bars\n" +
                "            updateBar('vida', stats.vida, maxStat);\n" +
                "            updateBar('escudo', stats.escudo, maxStat);\n" +
                "            updateBar('ataque', stats.ataque, maxStat);\n" +
                "            updateBar('critico', stats.critico, maxStat);\n" +
                "            \n" +
                "            // Update equipment list\n" +
                "            const equipmentList = document.getElementById('equipmentList');\n" +
                "            if (stats.decoradores && stats.decoradores.length > 0) {\n" +
                "                equipmentList.innerHTML = stats.decoradores.join(', ');\n" +
                "            } else {\n" +
                "                equipmentList.innerHTML = 'Ninguno';\n" +
                "            }\n" +
                "            \n" +
                "            // Update onion display\n" +
                "            updateOnionDisplay(stats);\n" +
                "            \n" +
                "            // Update hero visual based on swords\n" +
                "            updateHeroVisual(stats);\n" +
                "        }\n" +
                "\n" +
                "        function updateOnionDisplay(stats) {\n" +
                "            const onionLayers = document.getElementById('onionLayers');\n" +
                "            let layersHTML = '<div class=\"layer layer-base\">' + stats.base + '</div>';\n" +
                "            \n" +
                "            if (stats.decoradores && stats.decoradores.length > 0) {\n" +
                "                for (let i = stats.decoradores.length - 1; i >= 0; i--) {\n" +
                "                    const decorator = stats.decoradores[i];\n" +
                "                    let layerClass = 'layer-' + decorator.toLowerCase();\n" +
                "                    if (decorator.toLowerCase().includes('buff')) {\n" +
                "                        layerClass = 'layer-buff';\n" +
                "                    }\n" +
                "                    layersHTML += '<div class=\"layer ' + layerClass + '\">' + decorator + '</div>';\n" +
                "                }\n" +
                "            }\n" +
                "            \n" +
                "            onionLayers.innerHTML = layersHTML;\n" +
                "        }\n" +
                "\n" +
                "        function updateHeroVisual(stats) {\n" +
                "            const hero = document.getElementById('hero');\n" +
                "            // Remove sword classes\n" +
                "            hero.classList.remove('sword-1', 'sword-2', 'sword-3');\n" +
                "            \n" +
                "            // Count swords\n" +
                "            let swordCount = 0;\n" +
                "            if (stats.decoradores) {\n" +
                "                swordCount = stats.decoradores.filter(d => d === 'Espada').length;\n" +
                "            }\n" +
                "            \n" +
                "            // Add appropriate sword class\n" +
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
                "                headers: {\n" +
                "                    'Content-Type': 'application/x-www-form-urlencoded',\n" +
                "                },\n" +
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
                "                showMessage('Error al equipar ítem: ' + error, 'error');\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function atacar() {\n" +
                "            fetch('/atacar', {\n" +
                "                method: 'POST',\n" +
                "                headers: {\n" +
                "                    'Content-Type': 'application/x-www-form-urlencoded',\n" +
                "                }\n" +
                "            })\n" +
                "            .then(response => response.json())\n" +
                "            .then(data => {\n" +
                "                if (data.ganador) {\n" +
                "                    const logContent = document.getElementById('logContent');\n" +
                "                    const logEntry = document.createElement('div');\n" +
                "                    logEntry.innerHTML = `\n" +
                "                        <strong>⚔️ Combate contra ${data.enemigo.nombre}</strong><br>\n" +
                "                        Vida enemigo: ${data.enemigo.vida}/${data.enemigo.maxVida}<br>\n" +
                "                        Ataque enemigo: ${data.enemigo.ataque} | Escudo: ${data.enemigo.escudo}<br>\n" +
                "                        <strong>Ganador: ${data.ganador}</strong> (Turnos: ${data.turnos})\n" +
                "                    `;\n" +
                "                    logContent.insertBefore(logEntry, logContent.firstChild);\n" +
                "                    \n" +
                "                    // Update detailed combat log\n" +
                "                    updateDetailedCombatLog(data.detallesLog);\n" +
                "                    \n" +
                "                    showMessage(`¡${data.ganador} ganó el combate!`, data.ganador === 'HeroeBase' ? 'success' : 'info');\n" +
                "                    updateStatsDisplay(data.heroStats);\n" +
                "                } else if (data.error) {\n" +
                "                    showMessage(data.error, 'error');\n" +
                "                }\n" +
                "            })\n" +
                "            .catch(error => {\n" +
                "                showMessage('Error al atacar: ' + error, 'error');\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function updateDetailedCombatLog(detalleLog) {\n" +
                "            const detailedLogContent = document.getElementById('detailedLogContent');\n" +
                "            detailedLogContent.innerHTML = '';\n" +
                "            \n" +
                "            if (detalleLog && detalleLog.length > 0) {\n" +
                "                detalleLog.forEach(entry => {\n" +
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
                "                    detailedLogContent.appendChild(logEntry);\n" +
                "                });\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function stressTest() {\n" +
                "            showMessage('🔥 Iniciando prueba de estrés...', 'info');\n" +
                "            const items = ['espada', 'escudo', 'poder', 'buffataque', 'buffmultiplicador'];\n" +
                "            let delay = 0;\n" +
                "            \n" +
                "            for (let i = 0; i < 10; i++) {\n" +
                "                setTimeout(() => {\n" +
                "                    const randomItem = items[Math.floor(Math.random() * items.length)];\n" +
                "                    equipar(randomItem);\n" +
                "                }, delay);\n" +
                "                delay += 100;\n" +
                "            }\n" +
                "            \n" +
                "            setTimeout(() => {\n" +
                "                showMessage('✅ Prueba de estrés completada', 'success');\n" +
                "            }, delay + 500);\n" +
                "        }\n" +
                "\n" +
                "        function reset() {\n" +
                "            if (confirm('¿Estás seguro de que quieres resetear el héroe?')) {\n" +
                "                fetch('/reset', {\n" +
                "                    method: 'POST',\n" +
                "                    headers: {\n" +
                "                        'Content-Type': 'application/x-www-form-urlencoded',\n" +
                "                    }\n" +
                "                })\n" +
                "                .then(response => response.json())\n" +
                "                .then(data => {\n" +
                "                    if (data.message) {\n" +
                "                        showMessage(data.message, 'success');\n" +
                "                        updateStatsDisplay(data.stats);\n" +
                "                        updateHeroPosition(250, 250);\n" +
                "                        document.getElementById('logContent').innerHTML = 'Esperando acciones...';\n" +
                "                        document.getElementById('detailedLogContent').innerHTML = 'Esperando combate...';\n" +
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
                "        // Auto-update stats every 5 seconds\n" +
                "        setInterval(updateStats, 5000);\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
    
    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
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
