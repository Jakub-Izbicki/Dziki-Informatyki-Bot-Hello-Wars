package bot;

import com.google.gson.Gson;
import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
//you need to add com/sun/net/httpserver/** to project properties->JavaBuildPath->Librarys->Acces Rule - > Edit -> Add -> com/sun/net/httpserver/**
// ane change there to accessible

public class TankBlasterBot {
	private static HttpServer _botServer;

	public static void main(String[] args) throws Exception {
		_botServer = HttpServer.create(new InetSocketAddress(9065), 0);

		_botServer.createContext("/info", new HttpHandler() {
			@Override
			public void handle(HttpExchange httpExchange) throws IOException {
				BotInfo botInfo = new BotInfo();
				botInfo.AvatarUrl = "http://4.bp.blogspot.com/-3uACfJ0svMs/U0ExmWSHbKI/AAAAAAAADWY/bVBILkcvQ3s/s1600/dzik4.jpg";
				botInfo.Name = "Dzik 102";
				botInfo.GameType = "TankBlaster";
				botInfo.Description = "Szukam loszki";

				try {
					Gson gson = new Gson();
					String JsonResponse = gson.toJson(botInfo);

					Headers headers = httpExchange.getResponseHeaders();
					headers.set("Content-Type", "application/json");
					httpExchange.sendResponseHeaders(200, JsonResponse.length());
					OutputStream os = httpExchange.getResponseBody();

					os.write(JsonResponse.getBytes());
					os.close();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		_botServer.createContext("/close", new HttpHandler() {
			@Override
			public void handle(HttpExchange httpExchange) throws IOException {
				_botServer.stop(1);
			}
		});

		HttpContext performNextMoveContext = _botServer.createContext(
				"/PerformNextMove", new HttpHandler() {
					@Override
					public void handle(HttpExchange httpExchange)
							throws IOException {
						BotMove botMove = new BotMove();
						Gson gson = new Gson();

						try {
							Map params = (Map) httpExchange
									.getAttribute("parameters");

							Object object = params.keySet().iterator().next();
							String jsonString = params.keySet().toString();
							String jsonStringCutted = jsonString.substring(1,
									jsonString.length() - 1);
							
							BotArenaInfo botArenaInfo = gson.fromJson(
									jsonStringCutted, BotArenaInfo.class);

							BotLogic botLogic = new BotLogic(botArenaInfo);
							botMove = botLogic.CalculateNextMove();

							String JsonResponse = gson.toJson(botMove);

							Headers headers = httpExchange.getResponseHeaders();
							headers.set("Content-Type", "application/json");
							httpExchange.sendResponseHeaders(200,
									JsonResponse.length());
							OutputStream os = httpExchange.getResponseBody();

							os.write(JsonResponse.getBytes());
							os.close();

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
		
		performNextMoveContext.getFilters().add(new ParameterFilter());

		_botServer.setExecutor(null);
		_botServer.start();
	}
}
