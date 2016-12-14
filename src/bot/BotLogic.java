package bot;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

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

	public boolean IsInDangerZone(Point location, List<Bomb> bombsList) {
		if (!IsLocationValid(location)) {
			return true;
		}

		for (Bomb bomb : bombsList) {

			List<Point> dangerZone;
			dangerZone = GetDangerZone(ParsePoint(bomb.Location),
					bomb.ExplosionRadius);
			for (Point dangerZonePoint : dangerZone) {

				if (dangerZonePoint.x == location.x
						&& dangerZonePoint.y == location.y) {
					//jesteśmy w polu rażenia tej bomby, ale
					//jezeli bomba nie wybuchnie w nastepnej rundzie, to nie ma po co uciekać

                    if((bomb.RoundsUntilExplodes <= 2) && (location.x == bomb.LocationToPoint().x) && (location.y == bomb.LocationToPoint().y))
                        return true;

					if (bomb.RoundsUntilExplodes > 1){

						//ale trzeba jeszcze sprawdzić reakcję łańcuchową
						//czyli, czy bomba jest w zasięgu innej bomby, która wybuchnie w następnej rundzie

                        //TO recurence:
						List<Bomb> newBombsList = bombsList.stream()
								.filter(a -> !Objects.equals(a.Location, bomb.Location))
								.collect(Collectors.toList());

						if(newBombsList.size() == 0){
							return false;
						}

						if (IsInDangerZone(bomb.LocationToPoint(), newBombsList)){

							return true;
						}

						//nie ma reakcji łańcuchowej
						return false;
					}

					return true;
				}
			}
		}

		for (Missile missile : _field.Missiles) {
			List<Point> dangerZone;

			Point nextMissilesPosition = AddDirectionMove(ParsePoint(missile.Location),missile.MoveDirection);

			//W zależności od trybu gry tzn. IsFastMissileModeEnabled == true
			if(_field.GameConfig.IsFastMissileModeEnabled == true)
			{
				Point nextNextMissilesPosition = AddDirectionMove(nextMissilesPosition,missile.MoveDirection);

                //TODO : POINT EQUAL
                //jeśli rakieta w następnej nastepnej turze będzie na pozycji, na którą chcemy wejść
                if( nextNextMissilesPosition == location) return true;

                //Jeśli rakieta w następnej rundzie poleci, ale dalej będzie kolizja to wybucha od razu
				if(_field.Board[nextNextMissilesPosition.x][nextNextMissilesPosition.y] != 0)
				{
					dangerZone = GetDangerZone(nextMissilesPosition,
							missile.ExplosionRadius);
					for (Point dangerZonePoint : dangerZone) {

						if (dangerZonePoint.x == location.x
								&& dangerZonePoint.y == location.y) {
							return true;
						}
					}
				}
			}
			else
            {
                //TODO : POINT EQUAL
                //jeśli rakieta w nastepnej turze będzie na pozycji, na którą chcemy wejść
                if( nextMissilesPosition == location) return true;

                //Jeśli rakieta w następnej rundzie nie poleci dalej (bd miała kolizję) i wybuchnie
                if(_field.Board[nextMissilesPosition.x][nextMissilesPosition.y] != 0)
                {
                    dangerZone = GetDangerZone(nextMissilesPosition,
                            missile.ExplosionRadius);
                    for (Point dangerZonePoint : dangerZone) {

                        if (dangerZonePoint.x == location.x
                                && dangerZonePoint.y == location.y) {
                            return true;
                        }
                    }
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

	public List<Point> calculateMove(List<Point> oldMovementPoints){

//		List<Point> newMovementPoints = new ArrayList<Point>();

		for (Point oldPoint : oldMovementPoints){

			for (Bomb bomb : _field.Bombs){

				if (oldPoint.getLocation() == bomb.LocationToPoint()){

				    //TODO : CHyba usunąć ?
					if (bomb.RoundsUntilExplodes < 4 ){

						oldMovementPoints = oldMovementPoints.stream()
								.filter(a -> !Objects.equals(a.getLocation(), bomb.LocationToPoint()))
								.collect(Collectors.toList());
					}
				}
			}
		}

		return oldMovementPoints;
	}

	MoveDirection calculateFireDirection(Point op, Point me)
	{
		if(Math.abs(op.x - me.x) > Math.abs(op.y-me.y))
		{
			if(me.x>op.x)
				return MoveDirection.Left;
			else
				return MoveDirection.Right;
		}
		else
			if(me.y>op.y)
				return MoveDirection.Up;
			else
				return MoveDirection.Down;
	}

	public BotMove CalculateNextMove() {
		BotMove result = new BotMove();

		int randAction = rand.nextInt(9);

		result.FireDirection = calculateFireDirection(_field.GetOpponentLocationList(),_field.GetBotLocation());

		if (randAction == 0) {
			if(!IsInDangerZone(_field.GetBotLocation(), _field.Bombs))
				result.Action = BotAction.DropBomb;
		} else {
			result.Action = BotAction.None;
		}

	public List<MoveDirection> calculateOneStep(BotArenaInfo _currentField){

		List<MoveDirection> safeZones = new ArrayList<MoveDirection>();

		List<Point> movementPoints = new ArrayList<Point>();

		for (MoveDirection direction : MoveDirection.values()) {

			Point point = AddDirectionMove(_field.GetBotLocation(), direction);

			if (!IsInDangerZone(point, _field.Bombs)) {
				//safeZones.add(direction);
				movementPoints.add(point);
			}
		}

		movementPoints = calculateMove(movementPoints);

		for (MoveDirection direction : MoveDirection.values()) {

			Point point = AddDirectionMove(_field.GetBotLocation(), direction);

			if (movementPoints.contains(point)&&_field.Board[point.x][point.y] == 0) {
				safeZones.add(direction);
			}
		}

		return safeZones;
	}

	public BotArenaInfo getNextField(BotArenaInfo _oldField){

		BotArenaInfo _newField = _oldField;

		_newField.RoundNumber++;

		//Bombs:
		_newField.Bombs = _newField.Bombs.stream()
				.filter(bomb -> !Objects.equals(bomb.RoundsUntilExplodes, 1))
				.collect(Collectors.toList());

		for (Bomb bomb : _newField.Bombs){

			bomb.RoundsUntilExplodes--;
		}

		//increasing RoundsBeforeIncreasingBlastRadius:
		if ((_newField.RoundNumber % _newField.GameConfig.RoundsBeforeIncreasingBlastRadius) == 0)
			_newField.GameConfig.RoundsBeforeIncreasingBlastRadius++;

		//Missiles:

	}

	public BotMove CalculateNextMove() {
		BotMove result = new BotMove();

		int randAction = rand.nextInt(9);

		if (randAction == 0) {
			if(!IsInDangerZone(_field.GetBotLocation(), _field.Bombs))
				result.Action = BotAction.DropBomb;
		} else {
			result.Action = BotAction.None;
		}

		result.FireDirection = MoveDirection.Up;

		List<MoveDirection> safeZones = new ArrayList<MoveDirection>();

		safeZones = calculateOneStep(_field);

		if (safeZones.stream().count() > 0) {
			int randMoveAction = rand.nextInt((int) (safeZones.stream().count()));
			result.Direction = safeZones.get(randMoveAction);
		}


		return result;
	}

	private Point ParsePoint(String string) {
		String[] coords = string.split(",");
		return new Point(Integer.parseInt(coords[0].trim()),
				Integer.parseInt(coords[1].trim()));
	}
}
