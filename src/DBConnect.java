import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 
 * Used to manage DataBase
 * 1. create instance
 * 2. connect();
 * 3. do your biz
 * 4. close();
 * 
 * Limited user for SELECT
 * nstanish_user
 * xiySbU1[qk~n
 *
 */

public class DBConnect {
	private String username, password;
	private Connection c;
	public static final int ADMIN = 0;
	public static final int NORMAL = 1;
	public static final int ENCRYPTED = -1;
	public DBConnect(String username, String password) {
		c = null;
		this.username = username;
		this.password = password;
	}
	public boolean connect(){
		try {
            String url = "jdbc:mysql://javafilter.heliohost.org:3306/" + "nstanish_chatterbox" + "?useUnicode=true&characterEncoding=utf8&collation=UCS_BASIC ";
            Class.forName ("com.mysql.jdbc.Driver").newInstance ();
            c = DriverManager.getConnection (url, username, password);
            return true;
        }
        catch (Exception e){
            System.err.println ("Cannot connect to database server" + e);
            return false;
        }
	}
	public boolean close(){
		if (c != null)
        {
            try
            {
                c.close ();
                System.out.println ("Database connection terminated");
                return true;
            }
            catch (Exception e) { /* ignore close errors */ 
            	System.err.println("unable to close");
            	return false; //still connected
            }
        }
		return true;
	}
	
	public boolean createUser(String user, String pass, String name, String email, int type){
		try{
			pass = md5(pass);
			Statement st = c.createStatement();
      	  	String sql = "INSERT INTO auth (username, password, name, email, admin) VALUES ('"+ user +"','"+ pass +"','" + name + "','" + email + "','" + type +"')";
      	  	int returnValue = st.executeUpdate(sql);
      	  	return true;
		}
		catch (SQLException s){
			System.err.println("Unable to update:\n " + s);
			return false;
		}
  }
	public boolean createUser(String user, String pass, String name, String email){
		return createUser(user,pass,name,email,NORMAL);
	}
	public boolean login(String username, String password){
		return login(username, password, 0);
	}
	public boolean login(String username, String password, int encryption){
		ResultSet rs = selectUser(username);
		String pass;
		try {
			rs.first();
			pass = rs.getString("password");
			if(encryption != ENCRYPTED){
				password = md5(password);
			}
			if(password.equals(pass)){
				return true;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		return false;
	}
	public ResultSet selectUser(String username){
		//`username`, `password`, `name`, `email`, `admin`, 
		String sql;
		try {
			sql = "SELECT * FROM auth WHERE username='" + username+"'";
			ResultSet rs = c.createStatement().executeQuery(sql);
			return rs;
			/*
			while(rs.next()){
				int id  = rs.getInt("id");
				System.out.println("id =" + id);
				
			}
			return rs;
			*/
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public static String hex(byte[] array) {
		  StringBuffer sb = new StringBuffer();
		  for (int i = 0; i < array.length; ++i) {
		    sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).toUpperCase().substring(1,3));
		  }
		  return sb.toString();
		}
		 
	public static String md5(String message) { 
		  try { 
		    MessageDigest md = MessageDigest.getInstance("MD5"); 
		    return hex (md.digest(message.getBytes("CP1252"))); 
		  } catch (NoSuchAlgorithmException e) { } catch (UnsupportedEncodingException e) { } 
		  return null;
	}
	public static void main(String[] args) {
		DBConnect db = new DBConnect("nstanish_kagui", "xFcMC8V9MQT7");
		if (db.connect()){
			//create standard user
			/*
			if(!db.createUser("username", "password", "name", "email")){
				System.err.println("unable to create user");
			}
			else{
				
			}
			/*
			//create admin user
			if(!db.createUser("","","","",ADMIN)){
				System.err.println("unable to create user");
			}
			*/
			String username = "username";
			String password = "passord";
			if(db.login(username, md5(password))){
				System.out.println("login successful");
			}
			else{
				System.out.println("forgot your password?");
			}
				
			
		}
		else System.err.println("unable to connect");
		db.close();
	}
	

}
