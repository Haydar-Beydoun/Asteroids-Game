/*Asteroids
 *Haydar Beydoun
 **/

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.Random;
import java.util.Arrays;
import java.util.ArrayList;
import java.awt.event.*;

class Main{
	 public static void main(String [] args){
		Asteroids frame = new Asteroids();
	 }
}
class Asteroids extends JFrame{
	GamePanel game = new GamePanel();
	public Asteroids(){
		super("Asteroids");
		setSize(1200, 800);
		add(game);
		setVisible(true);
	}
}

class GamePanel extends JPanel implements KeyListener, ActionListener{
	private int[] playerX={ -3, -3, 22};//player polygon x coords
	private int[] playerY={ -9, 9, 0};//player polygon y coords
	private int[] livesX={ -9, 9, 0};//life icon x coords
	private int[] livesY={ 6, 6, -22};//life icon y coords
	private boolean[] keys;
	private boolean teleport, shoot, start;
	private double x, y, vx, vy;//x and y coords, velocities in x/y planes
	private long shootTimer, shootDelay, ufoDelay, ufoTimer, enemyshootTimer, enemyshootDelay;//different timer counters and delay values
	private int heading, lives, score, scoreGap, astCount;
	private ArrayList<Shot> shots = new ArrayList<Shot>();//arraylist of player shots
	private ArrayList<Shot> enemyshots = new ArrayList<Shot>();//arraylist of enemy shots
	private ArrayList<Asteroid> asts = new ArrayList<Asteroid>();//arraylist of asteroids
	private ArrayList<UFO> ufos = new ArrayList<UFO>();//arraylist of ufos
	Timer timer;
	Image logo;
	
	//Constructor
	public GamePanel(){
		keys = new boolean[KeyEvent.KEY_LAST+1];
		vx=0;
		vy=0;
		x = 585;
		y = 380;
		lives=3;
		score=0;
		heading=90;
		scoreGap=0;//keeps track of 1000 point gap-->used to change ast count
		astCount=10;//max number of asteroids on the screen
		teleport=false;
		shoot=false;
		start=false;
		ufoTimer=System.nanoTime();
		ufoDelay=30000;//30 second ufo spawn times
		shootTimer=System.nanoTime();
		shootDelay=175;
		enemyshootTimer=System.nanoTime();
		enemyshootDelay=3000;
		setFocusable(true);
		requestFocus();
		addKeyListener(this);
		timer = new Timer(20, this);
		logo = new ImageIcon("logo.png").getImage();
		timer.start();
	}

	//Helper functions
	private void fix(){//fix for heading angle
		heading= (heading%360+360) % 360;
	}
	public void turn(double degree){//adds value to heading based on input
		heading+=degree;
		fix();
	}
	public Polygon getPoly(){//generates a poly based on playerX and playerY coords and modifies them to incorporate rotation and current location
		int n = playerX.length;
		int [] ansx = new int[n];
		int [] ansy= new int[n];
		for(int i=0;i<n;i++){
			double []rot = rotate(playerX[i],playerY[i],heading);
			ansx[i] = (int)(rot[0] + x);
			ansy[i] = (int)(rot[1] + y);
		}
		return new Polygon(ansx, ansy, n);
	}
	public double []rotate(double x, double y, double ang){//rotate helper function
		double []v = vec(x,y);
		return xy(v[0], v[1]+ang);
	}
	public double []xy(double mag, double ang){//changes vector velue into its x and y components
		double [] ans = new double[2];
		ans[0] = Math.cos(Math.toRadians(ang))*mag;
		ans[1] = Math.sin(Math.toRadians(ang))*mag;
		return ans;
	}
	public double []vec(double x, double y){//changes x and y components into a vector
		double [] ans = new double[2];
		ans[0] = Math.sqrt(x*x + y*y);
		ans[1] = Math.toDegrees(Math.atan2(y,x));
		return ans;
	}

	//Ship abilities and resets
	public void thrust(){//ship movement function
		double [] move = xy(1, heading);//getting x and y values from desired vector
		vx += move[0];//adding components to their resepctive variables
		vy += move[1];
		double [] vels = cap(vx, vy);//calls cap which limits the velocity to a set max-->prevents infinite acceleration
		vx = vels[0];
		vy = vels[1];
	}
	public double[] cap(double x, double y){//velocity limiter
		double []mag=vec(x, y);//turns the x and y into a vector
		final double V_MAX=10;//vector speed limit

		if(mag[0]>V_MAX){
			mag[0]=V_MAX;
		}

		double [] vel = xy(mag[0], mag[1]);//change the limited vector into its x and y components
		return(vel);
	}
	public void teleport(){//random teleportation ability
		Random rand= new Random();
		x=rand.nextInt(getWidth()-200)+100;//generating a new pair of random coords
		y=rand.nextInt(getHeight()-100)+50;
	}
	public void shoot(){
		if(shoot){//if you are shooting-->used to prevent holding shoot
			long elapsed = (System.nanoTime()-shootTimer)/1000000;
			if(elapsed>shootDelay){//if more time has elapsed than the specified cooldown value
				shots.add(new Shot(x, y, heading));//add a Shot object to the shots class
				shootTimer=System.nanoTime();//reset the timer
			}
		}
	}
	public void move(){
		if(keys[KeyEvent.VK_UP]){//thrust movement
			thrust();
		}
		if(keys[KeyEvent.VK_RIGHT]){//turning right
			turn(10);
		}
		if(keys[KeyEvent.VK_LEFT]){//turning left
			turn(-10);
		}
		if(keys[KeyEvent.VK_DOWN] && teleport==false){//teleport by pressing down
			teleport=true;
			teleport();
		}
		if(keys[KeyEvent.VK_SPACE] && shoot==false){
			shoot=true;
			shoot();
		}

		x += vx;//constantly change the player coords by adding velocity components to them
		y += vy;
		
		//Inertia-->velocities are constantly slowly being decreased until they hit zero
		if(vx>0){
			vx-=.1;
			if(vx<0){
				vx=0;
			}
		}
		if(vx<0){
			vx+=0.1;
			if(vx>0){
				vx=0;
			}
		}
		if(vy>0){
			vy-=.1;
			if(vy<0){
				vy=0;
			}
		}
		if(vy<0){
			vy+=0.1;
			if(vy>0){
				vy=0;
			}
		}
		//wrap around screen
		if(x<0){
			x+=getWidth();
		}
		if(x>getWidth()){
			x-=getWidth();
		}
		if(y<0){
			y +=getHeight();
		}
		if(y>getHeight()){
			y-=getHeight();
		}
	}
	public void reset(){//resets all the variables to their starting values-->occurs after loss and fully resets the game
		vx=0;
		vy=0;
		x = 585;
		y = 380;
		lives=3;
		score=0;
		scoreGap=0;
		astCount=10;
		heading=90;
		ufoTimer=System.nanoTime();
		shootTimer=System.nanoTime();
		enemyshootTimer=System.nanoTime();

		shots.clear();
		enemyshots.clear();
		asts.clear();
		ufos.clear();
	}

	//Updaters/collision detection
	public void ufoUpdate(){
		//UFO
		long elapsed = (System.nanoTime()-ufoTimer)/1000000;
		if(elapsed>ufoDelay){//adding a ufo after specified cooldown has passed (30 seconds)
			ufos.add(new UFO());
			ufoTimer=System.nanoTime();
		}

		for(int i=0;i<ufos.size();i++){
			ufos.get(i).update();//move and variable updater of the ufos
			long enemyshotelapsed = (System.nanoTime()-enemyshootTimer)/1000000;
			if(enemyshotelapsed>enemyshootDelay){//shoot at the player after cooldown has passed
				enemyshots.add(new Shot(ufos.get(i).getx()+ufos.get(i).getr()*3/2,ufos.get(i).gety(),x, y));//adding Shot object to the enemyshots arraylist-->parameters apply to different constructor made specifically for the ufos
				enemyshootTimer=System.nanoTime();
			}
		}

		for(int i=0;i<shots.size();i++){//checking collision between player shots and the ufo
			Shot shot = shots.get(i);//getting every shot object in the array list
			double sx=shot.getx();//getting Shot object variables
			double sy=shot.gety();
			double sr=shot.getr();
			
			for(int j=0;j<ufos.size();j++){//cycling through ufos
				UFO ufo = ufos.get(j);
				double ux=ufo.getx();
				double uy=ufo.gety();
				double ur=ufo.getr();
				Area ufoArea = new Area(new Ellipse2D.Double(ux, uy, ur*3, ur));//area from ufo shape
				Area playerArea = new Area(getPoly());//area from player polygon
				if(ufoArea.intersects(sx, sy, sr, sr)){//if the ufo area intersects with the bullet
					shots.remove(i);//remove the shot
					i--;
					score+=200;//point increase
					scoreGap+=200;
					ufos.remove(j);//remove the ufo
					break;//break to prevent out of bounds error as you are constantly changing the array size
				}
			}
		}
	}
	public void shotUpdate(){
		for(int i=0;i<shots.size();i++){//cycling through player bullets
			boolean remove = shots.get(i).update();//update returns a boolean when the bullet timer runs out
			if(remove){//the bullets can only stay on screen for a certain amount of time
				shots.remove(i);//after the time ends, remove the bullets
				i--;
			}
		}
		for(int i=0;i<enemyshots.size();i++){//same concept but for enemy bullets
			boolean remove = enemyshots.get(i).update();
			if(remove){
				enemyshots.remove(i);
				i--;
			}
		}
	}
	public void astUpdate(){
		for(int i=0;i<asts.size();i++){//cycling through all asteroids
			asts.get(i).update();//constantly moving them and updating their variables
		}
		if(scoreGap>=1000){//every thousand points, increase the max asteroid count
			astCount+=2;//gradually make the game harder
			scoreGap=0;
		}
		if(asts.size()<astCount){//if there are not  enough asteroids on the screen, add them
			asts.add(new Asteroid());
		}
	}
	public void shotCollision(){
		//collision-->player shots and asteroids
		for(int i=0;i<shots.size();i++){//same logic as previous collision
			Shot shot = shots.get(i);
			double sx=shot.getx();
			double sy=shot.gety();
			double sr=shot.getr();

			for(int j=0;j<asts.size();j++){
				Asteroid ast = asts.get(j);
				double ax=ast.getx();
				double ay=ast.gety();
				double ar=ast.getr();

				Area astArea = new Area(ast.getPoly());
				if(astArea.intersects(sx, sy, sr, sr)){//if the asteroid area intersects with the bullets
					shots.remove(i);
					i--;
					score+=50;
					scoreGap+=50;
					if(ar>40){//if the radius of the asteroids is greater than 40, it will split into two new asteroids
						asts.add(new Asteroid(ax, ay, ar, ast.getangle()));//adds asts into the array (different constructor for split asteroids)
						asts.add(new Asteroid(ax, ay, ar, ast.getangle()));
					}
					asts.remove(j);
					break;
				}
			}
		}
		//collision-->enemy shots and asteroids
		for(int i=0;i<enemyshots.size();i++){//same logic as above but for the enemy shots
			Shot shot = enemyshots.get(i);
			double sx=shot.getx();
			double sy=shot.gety();
			double sr=shot.getr();

			for(int j=0;j<asts.size();j++){
				Asteroid ast = asts.get(j);
				double ax=ast.getx();
				double ay=ast.gety();
				double ar=ast.getr();

				Area astArea = new Area(ast.getPoly());
				if(astArea.intersects(sx, sy, sr, sr)){
					enemyshots.remove(i);
					i--;
					if(ar>40){//asts could split from enemy bullets
						asts.add(new Asteroid(ax, ay, ar, ast.getangle()));
						asts.add(new Asteroid(ax, ay, ar, ast.getangle()));
					}
					asts.remove(j);
					break;
				}
			}
		}
	}
	public void playerCollision(){
		//ship collision with ufo
		for(int j=0;j<ufos.size();j++){
			UFO ufo = ufos.get(j);
			double ux=ufo.getx();
			double uy=ufo.gety();
			double ur=ufo.getr();
			Area playerArea = new Area(getPoly());
			if(playerArea.intersects(ux, uy, ur*3, ur)){//if the player area intersects with the ufo bounds
				lives--;//decrease the lives
				if(lives<=0){//goes into gameover procedures
					reset();
					start=false;
					break;
				}
				x = 585;//spawning back in the center when hit
				y = 380;
				vx=0;
				vy=0;
				heading=90;
				ufos.remove(j);//ufo also dies
				break;
			}
		}
		//Player collision with enemy shots
		for(int i=0;i<enemyshots.size();i++){
			Shot shot = enemyshots.get(i);
			double sx=shot.getx();
			double sy=shot.gety();
			double sr=shot.getr();

			Area playerArea = new Area(getPoly());
			if(playerArea.intersects(sx, sy, sr, sr)){//if the player area hits the bullet bounds
				lives--;//same life loss procedure as before
				if(lives<=0){
					reset();
					start=false;
					break;
				}
				x = 585;
				y = 380;
				vx=0;
				vy=0;
				heading=90;
				enemyshots.remove(i);
				i--;
			}
		}
		//player collision-->between asteroids
		for(int j=0;j<asts.size();j++){
			Asteroid ast = asts.get(j);
			double ax=ast.getx();
			double ay=ast.gety();
			double ar=ast.getr();
			Area astArea = new Area(ast.getPoly());
			Area playerArea = new Area(getPoly());

			if(astArea.intersects(playerArea.getBounds())){//if the ast area collides with the bounds created around the ship area
				lives--;//same life loss procedure
				if(lives<=0){
					reset();
					start=false;
					break;
				}
				x = 585;
				y = 380;
				vx=0;
				vy=0;
				heading=90;
				if(ar>40){//asteroids break if they are big enough
					asts.add(new Asteroid(ax, ay, ar, ast.getangle()));
					asts.add(new Asteroid(ax, ay, ar, ast.getangle()));
				}
				asts.remove(j);
				break;
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e){
		repaint();
		if(start){
			move();
			ufoUpdate();
			shotUpdate();
			astUpdate();
			shotCollision();
			playerCollision();
		}
		else{
			ufoTimer=System.nanoTime();//dont let timers add up while game is not being played
		}
	}

	@Override
	public void keyPressed(KeyEvent ke){
		int key = ke.getKeyCode();
		keys[key]=true;
		if(key==KeyEvent.VK_ENTER){
			if(start==false){//if the game is not being played, start it
				asts.clear();//clear the preview screen (title with asteroids floating around)
			}
			start=true;
		}
	}
	@Override
	public void keyReleased(KeyEvent ke){
		int key = ke.getKeyCode();
		keys[key]=false;
		if(key==KeyEvent.VK_DOWN){
			teleport=false;//prevents key held issues
		}
		if(key==KeyEvent.VK_SPACE){
			shoot=false;//prevents key held issues
		}
	}
	@Override
	public void keyTyped(KeyEvent ke){}

	@Override
	public void paint(Graphics g){
		//Drawing background
		g.setColor(new Color(0, 0, 0));
		g.fillRect(0, 0, getWidth(), getHeight());

		//Drawing ship
		g.setColor(new Color(255, 255, 255));
		g.drawPolygon(getPoly());

		//Drawing UI
		if(start==false){
			//Start screen
			astUpdate();
			g.drawImage(logo , 230, 210, 723, 278, this);
			g.setFont(new Font("Calibri", Font.PLAIN, 20));
			g.drawString("Press Enter To Start", 520, getHeight()-100);
		}
		
		//Score text
		g.setFont(new Font("Calibri", Font.PLAIN, 20));
		g.drawString("Score: "+score, getWidth()-250, 35);
		
		//life icons
		if(lives>=3){
			Polygon lives = new Polygon(livesX, livesY, 3);
			lives.translate(getWidth()-30, 30);
			g.fillPolygon(lives);
		}
		if(lives>=2){
			Polygon lives = new Polygon(livesX, livesY, 3);
			lives.translate(getWidth()-60, 30);
			g.fillPolygon(lives);
		}
		if(lives>=1){
			Polygon lives = new Polygon(livesX, livesY, 3);
			lives.translate(getWidth()-90, 30);
			g.fillPolygon(lives);
		}

		//Array drawing loops-->cycle through each object and calls its draw function
		for(int i=0;i<shots.size();i++){
			shots.get(i).draw(g);
		}
		for(int i=0;i<enemyshots.size();i++){
			enemyshots.get(i).draw(g);
		}
		for(int i=0;i<asts.size();i++){
			asts.get(i).draw(g);
		}
		for(int i=0;i<ufos.size();i++){
			ufos.get(i).draw(g);
		}
	}

}

class UFO{
	private double x,y, speed, r;//coord variables, speed variable, radius variable
	private double[] xCoord={-50, 1250};//possible random spawn locations
	private double[] yCoord={100, 700};
	private double[] vDirection={-1, 1};//random direction
	Random rand= new Random();
	private int coordIndex = rand.nextInt(2);//choosing random indice from the arrays
	private int vDirIndex = rand.nextInt(2);

	public UFO(){
		speed=7*vDirection[vDirIndex];
		r=20;
		x=xCoord[coordIndex];
		y=yCoord[coordIndex];
	}

	//Helper functions
	public double getx(){
		return x;
	}
	public double gety(){
		return y;
	}
	public double getr(){
		return r;
	}

	//Coord. updater
	public void update(){
		x+=speed;//moving the ufos only in the x direction
		//screen wrapping
		if(x<0){
			x+=1200+r*3;
		}
		if(x>1200){
			x-=1200+r*3;
		}
	}

	//Drawing function-->called in paint
	public void draw(Graphics g){
		g.setColor(new Color(252, 186, 3));
		g.drawOval((int)(x), (int)(y), (int)r*3, (int)r);
		g.setColor(new Color(222, 187, 91));
		g.drawOval((int)(x), (int)(y)+10, (int)r*3, 0);
	}
}

class Asteroid{
	private double x,y;//coord variables
	private double dx,dy;//offset/velocity varibales
	private double speed, angle;
	private double r;//radius
	private double[] astX={-80, 400, 1270, 600, -80, -80};//random spawn coords
	private double[] astY={-80, -70, 300, 870, 870, 1100};
	private ArrayList<Integer> astXCoord= new ArrayList<Integer>();//hold polygon point coords
	private ArrayList<Integer> astYCoord= new ArrayList<Integer>();
	Random rand= new Random();
	private int astIndex = rand.nextInt(astX.length);//choosing random coords
	private Polygon ast =  new Polygon();//creating the asteroids

	//Constructors
	public Asteroid(){//initial asteroid creation constructor
		x=astX[astIndex];
		y=astY[astIndex];
		speed=rand.nextInt(6)+2;
		r=rand.nextInt(70)+20;
		angle=rand.nextInt(360);
		dx=Math.cos(Math.toRadians(angle))*speed;
		dy=Math.sin(Math.toRadians(angle))*speed;
		makeShape((int)x, (int)y, 9, 20, 70);//randomizing the asteroid
	}
	public Asteroid(double nx, double ny, double nr, double angle){//takes the broken asteroids coords, radius, and heading-->2nd constructor for asteroid splitting
		x=nx;//the new broken pieces have the same coords 
		y=ny;
		speed=rand.nextInt(5)+2;
		r=nr/2;
		angle*=rand.nextDouble()+0.6;
		dx=Math.cos(Math.toRadians(angle))*speed;
		dy=Math.sin(Math.toRadians(angle))*speed;
		makeShape((int)x, (int)y, 7, r-15, r);//randomizing asteroid shape
	}

	//Helper functions
	public double getx(){
		return x;
	}
	public double gety(){
		return y;
	}
	public double getr(){
		return r;
	}
	public double getangle(){
		return angle;
	}
	public Polygon getPoly(){//used to move the asteroid-->asteroids point coordinates and movement coordinates added
		int n = astXCoord.size();
		int[] ansx = new int[n];
		int[] ansy= new int[n];
		for(int i=0;i<n;i++){
			ansx[i]=(int)(astXCoord.get(i)+x);
			ansy[i]=(int)(astYCoord.get(i)+y);
		}
		return new Polygon(ansx, ansy, n);
	}

	//Coord. updater
	public void update(){
		x+=dx;
		y+=dy;
		//Screen wrapping
		if(x<0){
			x+=1200+r;
		}
		if(x>1200){
			x-=1200+r;
		}
		if(y<0){
			y +=800+r;
		}
		if(y>800){
			y-=800+r;
		}
	}

	//Polygon randomizer
	public void makeShape(int x, int y, int portions,double minR, double maxR){
		double portionAngle =2*Math.PI/portions;//split the polygon into a circle of equally cut portions
		for(int i=0;i<portions;i++){//move through the portion angle and randomize the radius of those portions
			double angle=portionAngle*i;//current portion updater
			double radius=rand.nextDouble()*(maxR-minR)+minR;
			double ax=Math.cos(angle)*radius;//getting x and y of the radius vector
			double ay=Math.sin(angle)*radius;
			ast.addPoint((int)ax, (int)ay);//add that point to the polygon
			astXCoord.add((int)ax);//storing the coordinates for makePolygon()
			astYCoord.add((int)ay);
		}
	}

	//Drawing function-->called in paint
	public void draw(Graphics g){
		g.setColor(new Color(255, 255, 255));
		g.drawPolygon(getPoly());
	}
}

class Shot{
	private double x,y;//x and y values
	private double dx,dy;//offset/velocity values
	private double speed;
	private int r;//radius
	private long shootTimer;
	private int shootMax;
	private Color colour;

	//Constructors
	public Shot(double x, double y, double angle){//player shot constructor
		this.x=x;//makes the coord values equal to the inputted ones
		this.y=y;
		r=5;
		speed=20;
		dx=Math.cos(Math.toRadians(angle))*speed;
		dy=Math.sin(Math.toRadians(angle))*speed;
		shootTimer=System.nanoTime();
		shootMax=1000;//playershots last a second
		colour= new Color(255, 255, 255);//colour is white for player bullets
	}
	public Shot(double x, double y, double px, double py){//enemy shot constructor
		this.x=x;
		this.y=y;
		r=5;
		speed=20;
		double angle=Math.atan2(py-y, px-x);//calculating angle between the ufo and player-->used to determine heading of the shot
		dx=Math.cos(angle)*speed;
		dy=Math.sin(angle)*speed;
		shootTimer=System.nanoTime();
		shootMax=1500;
		colour= new Color(252, 186, 3);
	}

	//Helper functions
	public double getx(){
		return x;
	}
	public double gety(){
		return y;
	}
	public double getr(){
		return r;
	}

	//Coord and timer updater
	public boolean update(){
		x+=dx;
		y+=dy;
		//Screen wrapping
		if(x<0){
			x+=1200;
		}
		if(x>1200){
			x-=1200;
		}
		if(y<0){
			y +=800;
		}
		if(y>800){
			y-=800;
		}
		
		// if the bullet has been on the screenn for the specified time
		long elapsed = (System.nanoTime()-shootTimer)/1000000;
		if(elapsed>shootMax){
			return true;//return true which would then remove the bullet from the arraylist in the gamePanel class
		}
		else{
			return false;
		}
	}

	//Drawing function-->called in paint
	public void draw(Graphics g){
		g.setColor(colour);
		g.fillOval((int)(x), (int)(y), r, r);
	}
}