//import com.google.gson.Gson; 

import java.util.ArrayList;

//import javax.json.JsonArray;

//Databse Connectivity

import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;

// JAX RS Modules
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

@Path("/")
public class ServerBoard {
	private static final int ZOMBIE_RATE = 10; //The lower, the faster.
	private static final String dbConnectionUrl = "jdbc:mysql://localhost:3306/swarm?autoReconnect=true&useSSL=false";
	private static final String dbConnectionUser = "root";
	private static final String dbConnectionPass = "root";
	private static final String connectString = dbConnectionUrl + "&user=" + dbConnectionUser + "&password=" + dbConnectionPass;
	//private static final String connectString = dbConnectionUrl  + "?useSSL=false&" + "?user=" + dbConnectionUser + "&password=" + dbConnectionPass;
	private static Connection connection;
	private static Statement statement;
	
	
	//The first 5 lines or so of the constructor connect to the database,
	//so we now know how to query the database.
	//Insertions & deletions work similarly, as demonstrated
	//in the ConnectTest class. 
	
	//We'll need to keep a list of sprites, then in the Tick() method,
	//run the Stalk method on each Zombie contained within
	private static ArrayList<Sprite> sprites;
	
	
	public ServerBoard() throws SQLException, ClassNotFoundException {
		sprites = new ArrayList<Sprite>();
		Class.forName("com.mysql.jdbc.Driver");
		connection = DriverManager.getConnection(connectString);
		statement = connection.createStatement(
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = statement.executeQuery("SELECT id, x_coord, y_coord FROM sprites");
		
		//After instantiating list, load from DB

		// Getting a SQL error during this loop.. before Result Set
		while (resultSet.next()) {
				String zid = (String) resultSet.getObject(1);
				int zx = Integer.parseInt(resultSet.getObject(2).toString());
				int zy = Integer.parseInt(resultSet.getObject(3).toString());
				if ( zid.charAt(0) == 'p' ){
					sprites.add(new Player(zid, zx, zy));
				} else {
					sprites.add(new Zombie(zid, zx, zy, 5, 5));
				}
			}
	}
	
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getBoard() {
		sprites = new ArrayList<Sprite>();
		try {
			Class.forName("com.mysql.jdbc.Driver");
		
		connection = DriverManager.getConnection(connectString);
		statement = connection.createStatement(
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		
		ResultSet resultSetPlayer = statement.executeQuery("SELECT id, x_coord, y_coord FROM sprites WHERE id LIKE \"p%\"");
		
		//Load Players onto list
		while (resultSetPlayer.next()) {
			String pid = (String) resultSetPlayer.getObject(1);
			int px = Integer.parseInt(resultSetPlayer.getObject(2).toString());
			int py = Integer.parseInt(resultSetPlayer.getObject(3).toString());
			sprites.add(new Player(pid, px, py));
		}
		resultSetPlayer.close();
		//Load Zombies into list
		ResultSet resultSet = statement.executeQuery("SELECT id, x_coord, y_coord, target_x, target_y FROM sprites WHERE id LIKE \"z%\"");
		while (resultSet.next()) {
				String zid = (String) resultSet.getObject(1);
				int zx = Integer.parseInt(resultSet.getObject(2).toString());
				int zy = Integer.parseInt(resultSet.getObject(3).toString());
				int zxTarg = Integer.parseInt(resultSet.getObject(4).toString());
				int zyTarg = Integer.parseInt(resultSet.getObject(5).toString());
				sprites.add(new Zombie(zid, zx, zy, zxTarg, zyTarg));
				
			}
		resultSet.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {

			e.printStackTrace();
		}
		
		
		
		
		
		String boardString = "" + sprites.size() + "&";
		for (Sprite s: sprites) {
			boardString+= s.getId();
			boardString += "-";
			boardString += s.getX();
			boardString += "-";
			boardString += s.getY();
			boardString += "&";
			
		}
		return boardString;
		
		
	}
	
	public boolean addZombie() {
		Zombie newZom = new Zombie();
		sprites.add(newZom);
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(connectString);
			statement = connection.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
			statement.execute("insert into sprites "
					+ "(id, x_coord, y_coord, target_x, target_y) values "
					+ "('"+ newZom.getId() + "'," + newZom.getX() + "," + newZom.getY()
					+ "," + newZom.getTargetX() + "," + newZom.getTargetY() + ");");
			System.out.println("Zombie added!");
			return true;	
		} catch (MySQLIntegrityConstraintViolationException msicve) {
			//id already in use, just call addZombie to increase the counter and try again until success.
			return addZombie();
		} catch (SQLException se) {
			se.printStackTrace();
			return false;
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
			return false;
		} 
	}
	
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public String updatePlayerPos(@QueryParam("id")   String id, 
								  @QueryParam("newX")   int finishX, 
								  @QueryParam("newY")	int finishY) throws ClassNotFoundException, SQLException 
	{
		try {
			Player newPlay = new Player(id, finishX, finishY);
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(connectString);
			statement = connection.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			sprites.add(newPlay);
			statement.execute("insert into sprites "
					+ "(id, x_coord, y_coord) values "
					+ "('"+ newPlay.getId() + "'," + newPlay.getX() + "," + newPlay.getY() + ");");
		} catch (MySQLIntegrityConstraintViolationException msicve) { //Player already in database, find em and move em
			try {
				String sqlCommand = "UPDATE sprites SET x_coord = " + finishX + ", y_coord = " + finishY
						+ " WHERE id = \"" + id + "\"";
				statement.execute(sqlCommand);
				sprites.add(new Player(id, finishX, finishY)); //This might create duplicates in the sprites list?
				
			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
			
		} catch (SQLException se) {
			se.printStackTrace();
			return null;
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
			return null;
		} 
		
		this.Tick(sprites);
		
		
		return getBoard();
	}
	/**
	 * This needs work. For starters, the tickCount static variable does not
	 * function correctly in this context. Statelessness and all that jazz.
	 * Not sure how I'm going to trigger this method then.
	 * 
	 * Second, it will need to be reworked. The steps necessary should be:
	 * 1) Pull all the Zombie data from database
	 * 2) Instantiate each zombie based on DB Data
	 * 3) Run the stalk algorithm on each, updating x, y, and target parameters
	 * 4) Save the zombie objects back to database
	 * 5) Return getBoard()
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 * 
	 */
	private void Tick(ArrayList<Sprite> sprites) throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		connection = DriverManager.getConnection(connectString);
		statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = statement.executeQuery("SELECT ticks FROM variables");
		int tickCount = 0;
		while (resultSet.next()) {
			tickCount = ((Long) resultSet.getObject(1)).intValue();
		}
		
		if (tickCount%ZOMBIE_RATE==0) {
			addZombie();
			tickCount = 0;
			
		}
		tickCount++;
		statement.execute("UPDATE variables SET ticks = " + tickCount + " WHERE idvariables = 1");
		System.out.println("Current tickCount is " + tickCount);
		
		
		
		
		
		//Update Positions & Targets	
		sprites = setTarget(sprites);
		
		//Save back to DB
		for (Sprite s : sprites) {
			
			if (s.getClass().equals(Zombie.class)) {//Update Zombie Positions
				statement.execute("UPDATE sprites SET x_coord = " + s.getX() +
						", y_coord = " + s.getY() + ", target_x = " + ((Zombie) s).getTargetX() + ", target_y = " + ((Zombie) s).getTargetY() +
						" WHERE id = \"" + s.getId() + "\"");
			}
			else {
				statement.execute("UPDATE sprites SET x_coord = " + s.getX() + 
						", y_coord = " + s.getY() + 
						" WHERE id = \"" + s.getId() + "\"");
			}
			
			
		}
		
		
		
	}
	
	/**
	 * Updates target and current x/y coordinates of zombies within the arrayList
	 * @param sprites
	 * @return
	 */
	private ArrayList<Sprite> setTarget(ArrayList<Sprite> sprites){
		for (Sprite s : sprites) {
			
			if (s.getClass().equals(Zombie.class)) {
				//System.out.println(((Zombie) s).getId()); //This works.
				for (Sprite sP: sprites) {
					if (sP.getClass().equals(Player.class)) {
						//System.out.println(((Player) sP).getId());
						((Zombie) s).setTarget(((Player) sP).getX(), ((Player) sP).getY());
						((Zombie) s).stalk();
					}
				}
				
			}
		}

		return sprites;
	}
	/** 
	 * Next Project
	 * @param id
	 * @return
	 */
	public boolean killSprite(String id) {
		
		
		
		return false;
	}
	
}