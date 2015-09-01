package com.floyd.bukkit.forum;


import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;


import java.util.regex.*;
import java.sql.*;

/**
* Approve plugin for Bukkit
*
* @author FloydATC
*/
public class ForumPlugin extends JavaPlugin {
    
	public static final String MSG_PREFIX = ChatColor.GRAY + "[" + ChatColor.GOLD + "Forum" + ChatColor.GRAY + "] ";
	public static final ChatColor COLOR_INFO = ChatColor.AQUA;

    private final ConcurrentHashMap<Player, Boolean> debugees = new ConcurrentHashMap<Player, Boolean>();
    public final ConcurrentHashMap<String, String> settings = new ConcurrentHashMap<String, String>();

    public static DbPool dbpool = null;
    
    String baseDir = "plugins/ForumPlugin";
    String configFile = "settings.txt";

	public static final Logger logger = Logger.getLogger("Minecraft.ApprovePlugin");
    
//    public ForumPlugin(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
//        super(pluginLoader, instance, desc, folder, plugin, cLoader);
//        // TODO: Place any custom initialization code here
//
//        // NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled
//    }

    public void onDisable() {
        // TODO: Place any custom disable code here
    	
        // NOTE: All registered events are automatically unregistered when a plugin is disabled
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
    	PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events

    	loadSettings();
    	initDbPool();
    	
    	// Set up a poll scheduler
    	this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
    		 
    	    @Override
    	    public void run() {
    	        checkForum();
    	    }
    	 
    	}, 0L, 600L); // 600L = 30 seconds
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }


    protected void checkForum() {
    	Connection dbh = null;
        PreparedStatement sth = null;

        if (dbpool == null) {
		    logger.info("[SMF] Retrying dbpool initialization");
		    initDbPool();
		}
		if (dbpool != null) {
			dbh = dbpool.getConnection();
	        if (dbh != null) {
       			try {
					sth = dbh.prepareStatement(settings.get("db_query"));
        			ResultSet rs = sth.executeQuery();
        			ResultSetMetaData rsMetaData = rs.getMetaData();
        			Object col[] = new Object[rsMetaData.getColumnCount()];
        			while (rs.next()) {
        				for (Integer i = 1; i < rsMetaData.getColumnCount()+1; i++) {
        					col[i-1] = rs.getString(i);
            	    		//logger.info("[SMF]   i="+i+" string="+rs.getString(i));
        				}
        				String str = String.format(settings.get("formatstring"), col);
        	    		logger.info("[SMF] "+str);
        				broadcast(str);
        			}
       			}
       			catch (SQLException e) {
					e.printStackTrace();
					logger.warning("[SMF] SQL error: "+e.getLocalizedMessage());
				}
       			dbpool.releaseConnection(dbh);
	        }
		}
	}

	private void initDbPool() {
    	try {
	    	dbpool = new DbPool(
	    		settings.get("db_url"), 
	    		settings.get("db_user"), 
	    		settings.get("db_pass"),
	    		Integer.valueOf(settings.get("db_min")),
	    		Integer.valueOf(settings.get("db_max"))
	    	);
    	} catch (RuntimeException e) {
    		logger.warning("[SMF] Init error: "+e.getLocalizedMessage());
    	}
    }
    
    
    
    public boolean isDebugging(final Player player) {
        if (debugees.containsKey(player)) {
            return debugees.get(player);
        } else {
            return false;
        }
    }

    public void setDebugging(final Player player, final boolean value) {
        debugees.put(player, value);
    }
    
    // Code from author of Permissions.jar
    
    private void loadSettings() {
    	String fname = baseDir + "/" + configFile;
		String line = null;

		// Load the settings hash with defaults
		settings.put("db_url", "");
		settings.put("db_user", "");
		settings.put("db_pass", "");
		settings.put("db_min", "2");
		settings.put("db_max", "10");
		settings.put("db_query", "SELECT real_name, subject, id_topic, id_msg, id_msg FROM messages LEFT JOIN members ON (members.member_name = messages.poster_name) WHERE FROM_UNIXTIME(poster_time+30) > NOW() ORDER BY poster_time DESC LIMIT 3");
		settings.put("formatstring", "%s posted '%s'\nhttp://forums.atc.no/index.php?topic=%s.msg%s#msg%s");
		// Read the current file (if it exists)
		try {
    		BufferedReader input =  new BufferedReader(new FileReader(fname));
    		while (( line = input.readLine()) != null) {
    			line = line.trim();
    			if (!line.startsWith("#") && line.contains("=")) {
    				String[] pair = line.split("=", 2);
    				settings.put(pair[0], fix_escapes(pair[1]));
    			}
    		}
    		input.close();
    	}
    	catch (FileNotFoundException e) {
			logger.warning( "[SMF] Error reading " + e.getLocalizedMessage() + ", using defaults" );
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    private void broadcast(String message) {
    	for (Player p : this.getServer().getOnlinePlayers()) {
    		send(p, message);
    	}
    }
    
    private void send(Player player, String message) {
    	if (player == null) {
        	// Strip color codes
        	Pattern pattern = Pattern.compile("\\§[0-9a-f]");
        	Matcher matcher = pattern.matcher(message);
        	message = matcher.replaceAll("");
        	// Print message to console
    		System.out.println(message);
    	} else {
    		player.sendMessage(MSG_PREFIX+COLOR_INFO+message);
    	}
    }
    
    private String fix_escapes(String str) {
    	str = str.replaceAll("\\\\n", "\n"); // Fix newline
    	return str;
    }

}

