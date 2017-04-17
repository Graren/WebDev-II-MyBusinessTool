package drog.web2.api;

import java.io.IOException;
import java.sql.Timestamp;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.websocket.Session;

import darb.web2.JDBConnection;
import darb.web2.yayson.YaySon;
import darb.web2.yayson.YaySonArray;
import drog.web2.NotificationSocket;
import drog.web2.User;

/**
 * Servlet implementation class Project
 */
@WebServlet("/api/project")
public class Project extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Project() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession();		
		response.setHeader("Content-Type", "application/json" );
		YaySon res = new YaySon();
		
		if (session.isNew()) {
			res.add("status", 403);
			res.add("error", "No permission to access this resource");
		} else {
			String id_project = request.getParameter("id_project");
		
			if (id_project != null) {
				try {
					String query = "SELECT id_project, name, description, created_at, id_leader FROM project where id_project=?";
					JDBConnection conn = new JDBConnection("localhost", 5432, "my_business_tool", "postgres", "masterkey");
					String[][] table = conn.executeQuery(query, Integer.parseInt(id_project));
					YaySon project = new YaySon();
					project.add("id_project", Integer.parseInt(id_project));
					project.add("name", table[1][1]);
					project.add("description", table[1][2]);
					project.add("created_at", table[1][3]);
					project.add("id_leader", Integer.parseInt(table[1][4]));
					res.add("status", 200);
					res.add("data", project);
				} catch(Exception e) {
					e.printStackTrace();
					res.add("status", 500);
					session.invalidate();
				}	
			} else {
				try {
					String query = "SELECT project.id_project, project.name, project.description, project.created_at, users.id_user, users.name FROM project INNER JOIN users ON project.id_leader = users.id_user";
					JDBConnection conn = new JDBConnection("localhost", 5432, "my_business_tool", "postgres", "masterkey");
					String[][] table = conn.executeQuery(query);
					YaySonArray projects = new YaySonArray();
					
					for(Integer i = 1 ; i < table.length ; i++){
						YaySon project = new YaySon();
						YaySon project_leader = new YaySon();
						project.add("id_project", Integer.parseInt(table[i][0]));
						project.add("name", table[i][1]);
						project.add("description", table[i][2]);
						project.add("created_at", table[i][3]);
						project_leader.add("id_user", Integer.parseInt(table[i][4]));
						project_leader.add("name", table[i][5]);
						project.add("project_leader", project_leader);
						projects.push(project);
					}
					
					res.add("status", 200);
					res.add("data", projects);
				} catch(Exception e) {
					e.printStackTrace();
					res.add("status", 500);
					session.invalidate();
				}
			}
		}		
		
		response.setStatus(res.getInteger("status"));
		response.getWriter().print(res.toJSON());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession();		
		User user = (User) session.getAttribute("user");
		response.setHeader("Content-Type", "application/json" );
		YaySon res = new YaySon();
		if (session.isNew() || user.getRoleId() != 1) {
			res.add("status", 403);
			res.add("error", "No permission to access this resource");
		} else {
			String name = request.getParameter("name");
			String description = request.getParameter("description");
			Integer id_leader = Integer.parseInt(request.getParameter("id_leader"));
			Timestamp now = new Timestamp(System.currentTimeMillis());
			String insertQuery = "INSERT INTO project(name, description, created_at, id_leader) VALUES (?,?,?,?) RETURNING id_project";
			String getQuery = "SELECT id_project, name, description, created_at FROM project WHERE id_project=?";
			String getLeaderQuery = "SELECT id_user, name, id_role FROM users WHERE id_user=?";
			try {
				JDBConnection conn = new JDBConnection("localhost", 5432, "my_business_tool", "postgres", "masterkey");
				String[][] leaderTable = conn.executeQuery(getLeaderQuery, id_leader);
				Integer project_leader_role_id = Integer.parseInt(leaderTable[1][2]);
				if (project_leader_role_id <= 2) {
					String[][] insert_result = conn.executeQuery(insertQuery, name, description, now, id_leader);
					Integer id_project = Integer.parseInt(insert_result[1][0]);
					String[][] projectTable = conn.executeQuery(getQuery, id_project);
					try {
						NotificationSocket.sendNotification(id_leader, "You've been assigned as the leader to Project: \"" + name +"\"", "info");
					} catch(NullPointerException | IOException e) {
						System.out.println("User to be notified is not online.");
					}
					YaySon project = new YaySon();
					YaySon project_leader = new YaySon();
					project.add("id_project", id_project);
					project.add("name", projectTable[1][1]);
					project.add("description", projectTable[1][2]);
					project.add("created_at", projectTable[1][3]);
					project_leader.add("id_user", id_leader);
					project_leader.add("name", leaderTable[1][1]);
					project.add("project_leader", project_leader);
					res.add("status", 200);
					res.add("data", project);
				} else {
					res.add("status", 400);
					res.add("error", "Assigned leader has to be at least a manager");
				}
				
			} catch(Exception e) {
				e.printStackTrace();
				res.add("status", 500);
			}
		}		
		
		response.setStatus(res.getInteger("status"));
		response.getWriter().print(res.toJSON());
	}

}
