package bot;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BotLogic {

	BotArenaInfo _field;
	Random rand = new Random();

	BotLogic(BotArenaInfo field) {
		_field = field;
	}

	public List<Point> GetSurroundingPoints(Point centerLocation, int radius) {
		List<Point> result = new ArrayList<Point>();

		for (int i = 1; i <= radius; i++) {
			Point p1 = new Point(centerLocation.x, centerLocation.y + i);
			if (IsLocationValid(p1))
				result.add(p1);

			Point p2 = new Point(centerLocation.x, centerLocation.y - i);
			if (IsLocationValid(p2))
				result.add(p2);

			Point p3 = new Point(centerLocation.x + i, centerLocation.y);
			if (IsLocationValid(p3))
				result.add(p3);

			Point p4 = new Point(centerLocation.x - i, centerLocation.y);
			if (IsLocationValid(p4))
				result.add(p4);
		}

		return result;
	}

	private boolean IsLocationValid(Point location) {
		return location.x >= 0 && location.x < _field.Board[0].length
				&& location.y >= 0 && location.y < _field.Board[1].length;
	}

	public boolean IsInDangerZone(Point location) {
		if (!IsLocationValid(location)) {
			return true;
		}

		for (Bomb bomb : _field.Bombs) {



			List<Point> dangerZone;
			dangerZone = GetDangerZone(ParsePoint(bomb.Location),
					bomb.ExplosionRadius);
			for (Point dangerZonePoint : dangerZone) {

				if (dangerZonePoint.x == location.x
						&& dangerZonePoint.y == location.y) {
					//jesteśmy w polu rażenia tej bomby, ale
					//jezeli bomba nie wybuchnie w nastepnej rundzie, to nie ma po co uciekać
					if (bomb.RoundsUntilExplodes > 1){

						//ale trzeba jeszcze sprawdzić reakcję łańcuchową
						//czyli, czy bomba jest w zasięgu innej bomby, która wybuchnie w następnej rundzie
//						if (IsInDangerZone(bomb.LocationToPoint())){
//
//							return true;
//						}

						//nie ma reakcji łańcuchowej
						return false;
					}

					return true;
				}
			}
		}

		for (Missile missile : _field.Missiles) {
			List<Point> dangerZone;

			//jeśli bomba w nastepnej turze będzie na pozycji, na którą chcemy wejść
			if(AddDirectionMove(ParsePoint(missile.Location),missile.MoveDirection) == location) return true;

			dangerZone = GetDangerZone(ParsePoint(missile.Location),
					missile.ExplosionRadius);
			for (Point dangerZonePoint : dangerZone) {

				if (dangerZonePoint.x == location.x
						&& dangerZonePoint.y == location.y) {
					return true;
				}
			}
		}

		if (_field.Board[location.x][location.y] != 0) {
			return true;
		}

		return false;
	}

	private List<Point> GetDangerZone(Point centerLocation, int explosionRadius) {
		List<Point> result = GetSurroundingPoints(centerLocation,
				explosionRadius);
		result.add(centerLocation);

		return result;
	}

	public Point AddDirectionMove(Point location, MoveDirection direction) {
		Point result = new Point(location.x, location.y);

		switch (direction) {
			case Up:
				result.y--;
				break;

			case Down:
				result.y++;
				break;

			case Right:
				result.x++;
				break;

			case Left:
				result.x--;
				break;
		}

		return result;
	}

	public BotMove CalculateNextMove() {
		BotMove result = new BotMove();

		int randAction = rand.nextInt(9);

		if (randAction == 0) {
			result.Action = BotAction.DropBomb;
		} else {
			result.Action = BotAction.None;
		}

		result.FireDirection = MoveDirection.Up;

		List<MoveDirection> safeZones = new ArrayList<MoveDirection>();

		for (MoveDirection direction : MoveDirection.values()) {
			if (!IsInDangerZone(AddDirectionMove(_field.GetBotLocation(),
					direction))) {
				safeZones.add(direction);
			}
		}

		if (safeZones.stream().count() > 0) {
			int randMoveAction = rand.nextInt((int) (safeZones.stream().count()));
			result.Direction = safeZones.get(randMoveAction);
		}

//		result.Action = BotAction.FireMissile;
//		result.FireDirection = MoveDirection.Down;

//		for(int[] arr : _field.Board){
//
//			System.out.print("[");
//
//			for(int field : arr){
//
//				System.out.print(field);
//			}
//			System.out.println("]");
//		}

		return result;
	}

	private Point ParsePoint(String string) {
		String[] coords = string.split(",");
		return new Point(Integer.parseInt(coords[0].trim()),
				Integer.parseInt(coords[1].trim()));
	}
}
