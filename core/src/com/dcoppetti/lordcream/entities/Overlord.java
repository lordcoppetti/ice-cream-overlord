package com.dcoppetti.lordcream.entities;

import static com.dcoppetti.lordcream.IceCreamOverlordGame.PPM;
import net.dermetfan.gdx.graphics.g2d.Box2DSprite;
import net.dermetfan.gdx.physics.box2d.Box2DUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

/**
 * @author Diego Coppetti
 */
public class Overlord extends Box2DSprite implements GameEntity {


    public enum PlayerState {
        Idle,
        Sliding,
        OnAir,
        OnWall
    }

	public static final String PLAYER_FOOTER = "PLAYER_FOOTER";
	public static final String PLAYER_SIDE = "PLAYER_SIDE";
	public int playerFootContanct;
	public boolean playerSideContact = false;

	// box2d stuff
    private Body body;
    private FixtureDef sensorFdef;
    private Fixture leftSide;
    private Fixture rightSide;

    private float colliderWidth;
    private float colliderHeight;
    private PlayerState state = PlayerState.Idle;
    private boolean canMove = true;
    private boolean facingLeft = false;

    private float slideAccel = 6f;
    private float maxSlideSpeed = 10f;
    private float jumpPower = 80f;
	private float jumpLimit = 3f;
	private float stickForce = 80f;
	private float stickFallForce = -0f; // this force would pull you down when stick to walls, thsi could be changed in certain levels
	
	// Animations
	private Animation idleAnim;
	private Animation slideAnim;
	private float idleAnimTimer = 0;
	private float slideAnimTimer = 0;
	
	// movement variables changed in the update movement method (don't touch here)
	float velX;
	float newX;
	float newY;


    public Overlord(World world, TextureRegion region, Vector2 position) {
        super(region);
        createBody(world, position);
    }
    
    public void setAnimationRegions(Array<TextureRegion> idleRegions, Array<TextureRegion> slideRegions) {
    	idleAnim = new Animation(0.35f, idleRegions);
    	idleAnim.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
    	setRegion(idleAnim.getKeyFrame(0));
    	
    	slideAnim = new Animation(0.1f, slideRegions);
    	slideAnim.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
    }

    private void createBody(World world, Vector2 position) {
		colliderWidth = getWidth()/3.6f/PPM;
		colliderHeight = getHeight()/4/PPM;
        BodyDef bdef = new BodyDef();
        bdef.position.set(position);
        bdef.type = BodyDef.BodyType.DynamicBody;
        bdef.fixedRotation = true;
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(colliderWidth/2, colliderHeight/2);
        FixtureDef fdef = new FixtureDef();
        fdef.friction = 0.2f;
        fdef.shape = shape;
        body = world.createBody(bdef);
        body.createFixture(fdef);
        shape.dispose();

		setUseOrigin(true);
		setAdjustSize(false);
		setOrigin(getWidth()/2, (getHeight()/2)+0.08f);
		setX(-getWidth()/2 + Box2DUtils.width(body) / 2);
		setY(-getHeight()/2 + Box2DUtils.height(body) / 2);
		setScale(getScaleX()/2/PPM, getScaleY()/2/PPM);

        body.setUserData(this);
        
        createFootSensor();
        createRightSensor();
    }

    private void createFootSensor() {
		sensorFdef = new FixtureDef();
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(colliderWidth/2.5f, colliderHeight/8f,
				new Vector2(0, -colliderHeight/2), 0);
		sensorFdef.isSensor = true;
		sensorFdef.shape = shape;
		body.createFixture(sensorFdef).setUserData(PLAYER_FOOTER);
		shape.dispose();
	}

    private void createLeftSensor() {
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(colliderWidth/8f, colliderHeight/2.5f,
				new Vector2(-colliderWidth/2f, 0), 0);
		sensorFdef.isSensor = true;
		sensorFdef.shape = shape;
		leftSide = body.createFixture(sensorFdef);
		leftSide.setUserData(PLAYER_SIDE);
		shape.dispose();
	}

	private void createRightSensor() {
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(colliderWidth/8f, colliderHeight/2.5f,
				new Vector2(colliderWidth/2f, 0), 0);
		sensorFdef.isSensor = true;
		sensorFdef.shape = shape;
		rightSide = body.createFixture(sensorFdef);
		rightSide.setUserData(PLAYER_SIDE);
		shape.dispose();
		
	}

	@Override
    public void update(float delta) {
    	updateState();
    	updateAnimations(delta);
    	updateSideFixture();
        updateMovement();
    }

	private void updateAnimations(float delta) {
		TextureRegion region = null;
		switch (state) {
		case Idle:
			slideAnimTimer = 0;
			idleAnimTimer += delta;
			region = idleAnim.getKeyFrame(idleAnimTimer);
			setRegion(region);
			break;
		case Sliding:
			idleAnimTimer = 0;
			slideAnimTimer += delta;
			region = slideAnim.getKeyFrame(slideAnimTimer);
			setRegion(region);
			break;
		default:
			break;
		}
		if(facingLeft) {
			setFlip(true, false);
		} else {
			setFlip(false, false);
		}
	}

	private void updateSideFixture() {
    	if(isFlipX() && rightSide != null) {
			body.destroyFixture(rightSide);
			rightSide = null;
			createLeftSensor();
    	}
    	if(!isFlipX() && leftSide != null) {
			body.destroyFixture(leftSide);
			leftSide = null;
			createRightSensor();
    	}
	}

	private void updateState() {
		if(playerSideContact && ! isGrounded()) {
				if(leftSide != null && Gdx.input.isKeyPressed(Keys.A) ||
					rightSide != null && Gdx.input.isKeyPressed(Keys.D)) {
					state = PlayerState.OnWall;
					return;
				}
		}
		if(isGrounded() && MathUtils.floor(body.getLinearVelocity().x) != 0) {
			state = PlayerState.Sliding;
			return;
		}
   		if(!isGrounded()) {
   			state = PlayerState.OnAir;
   			return;
   		}
   		state = PlayerState.Idle;
	}

	private void updateMovement() {
        if(!canMove) return;
        newX = 0;
        newY = 0;
        velX = body.getLinearVelocity().x;
        
        switch (state) {
		case OnWall:
			// Stick to walls
            if(!Gdx.input.isKeyPressed(Input.Keys.W)) {
            	stickToWall();
            } else {
            	WallJump();
            }
			break;
		case OnAir:
            if(Gdx.input.isKeyPressed(Input.Keys.A)) {
            	moveLeft();
            }
            if(Gdx.input.isKeyPressed(Input.Keys.D)) {
            	moveRight();
            }
            break;
		case Idle:
		case Sliding:
            if(Gdx.input.isKeyPressed(Input.Keys.W)) {
            	jump();
            }
            if(Gdx.input.isKeyPressed(Input.Keys.A)) {
            	moveLeft();
            }
            if(Gdx.input.isKeyPressed(Input.Keys.D)) {
            	moveRight();
            }
            break;
		default:
			break;
        }

        if(velX < -0.1f) {
            //this.setFlip(true, false);
            facingLeft = true;
        } else if(velX > 0.1f){
            //this.setFlip(false, false);
            facingLeft = false;
        }
        // This would be the traditional way of moving at a fixed pace
        //body.setLinearVelocity(x, y);
        // This way im applying a force making it feel like it's "sliding"
        newX = MathUtils.floor(newX);
        newY = MathUtils.floor(newY);
        body.applyForceToCenter(newX, newY, true);
    }
	
	private void WallJump() {
		if(leftSide != null) {
			if(body.getLinearVelocity().y < jumpLimit ) {
				body.applyForceToCenter(68, jumpPower, true);
			}
		} else if(rightSide != null) {
			if(body.getLinearVelocity().y < jumpLimit ) {
				body.applyForceToCenter(-68, jumpPower, true);
			}
		}
		
	}

	private void stickToWall() {
		if(leftSide != null && Gdx.input.isKeyPressed(Keys.A)) {
			newX = -stickForce;
		} else if(rightSide != null && Gdx.input.isKeyPressed(Keys.D)) {
			newX = stickForce;
		}
		newY = stickFallForce;
	}

	private void jump() {
		// Limit it or not?
		// Now limited to 20% higher
		if(body.getLinearVelocity().y < jumpLimit ) {
			body.applyForceToCenter(0, jumpPower, true);
		}
	}
	
	private void moveLeft() {
		if(Gdx.input.isKeyPressed(Input.Keys.A)) {
			if(velX - slideAccel >= -maxSlideSpeed) {
				newX = -slideAccel;
			}
		}
	}
	private void moveRight() {
		if(Gdx.input.isKeyPressed(Input.Keys.D)) {
			if(velX + slideAccel <= maxSlideSpeed) {
				newX = slideAccel;
			}
		}
	}

	public boolean isGrounded() {
		return this.playerFootContanct > 0;
	}

    @Override
    public void dispose() {

    }

	@Override
	public void collided(GameEntity b) {
		// TODO Auto-generated method stub
		
	}

	public PlayerState getState() {
		return state;
	}
	
	public Body getBody() {
		return body;
	}
}
