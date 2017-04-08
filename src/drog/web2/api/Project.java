package drog.web2.api;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;

import org.mindrot.BCrypt;

import darb.web2.JDBConnection;
import darb.web2.yayson.YaySon;
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
		User user = (User) session.getAttribute("user");
		response.setHeader("Content-Type", "application/json" );
		YaySon res = new YaySon();
		
		if (session.isNew() || user.getRoleId() != 1) {
			res.add("status", 403);
			res.add("error", "No permission to access this resource");
		} else {
			Integer id_project = null;
			
			try {
				id_project = Integer.parseInt(request.getParameter("id_project"));
			} catch(NumberFormatException e) {
				res.add("status", 403);
				res.add("error", "Invalid project id");
			}
			
			if (id_project != null) {
				try {
					String query = "SELECT id_project, name, description, created_at FROM project where id_project=?";
					JDBConnection conn = new JDBConnection("localhost", 5432, "my_business_tool", "postgres", "masterkey");
					String[][] table = conn.executeQuery(query, id_project);
					YaySon project = new YaySon();
					project.add("id_project", id_project);
					project.add("name", table[1][1]);
					project.add("description", table[1][2]);
					project.add("created_at", table[1][3]);
					res.add("status", 200);
					res.add("data", project);
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
			Timestamp now = new Timestamp(System.currentTimeMillis());
			String insertQuery = "INSERT INTO project(name, description, created_at) VALUES (?,?,?) returning id_project";
			String getQuery = "SELECT id_project, name, description, created_at FROM project where id_project=?";
			
			try {
				JDBConnection conn = new JDBConnection("localhost", 5432, "my_business_tool", "postgres", "masterkey");
				String[][] insert_result = conn.executeQuery(insertQuery, name, description, now);
				Integer id_project = Integer.parseInt(insert_result[1][0]);
				String[][] table = conn.executeQuery(getQuery, id_project);
				YaySon project = new YaySon();
				project.add("id_project", id_project);
				project.add("name", table[1][1]);
				project.add("description", table[1][2]);
				project.add("created_at", table[1][3]);
				res.add("status", 200);
				res.add("data", project);
			} catch(Exception e) {
				e.printStackTrace();
				res.add("status", 500);
			}
		}		
		
		response.setStatus(res.getInteger("status"));
		response.getWriter().print(res.toJSON());
	}

}
