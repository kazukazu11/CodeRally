import com.ibm.rally.Car;
import com.ibm.rally.ICar;
import com.ibm.rally.IObject;
/**
 * This is the class that you must implement to enable your car within
 * the CodeRally track. Adding code to these methods will give your car
 * it's personality and allow it to compete.
 */
public class RallyCar extends Car {
	/**
	 * @see com.ibm.rally.Car#getName()
	 */
	
	IObject checkPoints[];	//checkPointの情報
	IObject gasStations[];	//gasStationの情報
	ICar opponents[];		//他車の情報
	
	int nearNo;					//近いcheckPointの配列番号
	int prevNo;					//ひとつ前に通過したcheckPointの配列番号
	int waitBack;				//0以上なら後ろへ下がる
	boolean gasFlag;			//gasStationへ向かう
	boolean startFlag = false;	//初めてcheckPointへ向かう
	int Collided_count;         //衝突回数
	
	// RallyCarの名前を入力
	public String getName() {
		return "A";
	}

	// チームの名前を入力
	public String getSchoolName() {
		return "C-05";
	}

	// RallyCarの色を入力
	public byte getColor() {
		return CAR_RED;
	}

	public void initialize() {
		// put implementation here
		checkPoints = getCheckpoints();	
		gasStations = getFuelDepots();	
		prevNo = getPreviousCheckpoint();
		nearNo = getNearIObject(checkPoints);
		
		startFlag = true;		
		//チェックポイントが近すぎる場合は、次のチェックポイントへ
		if(getDistanceTo(checkPoints[nearNo])<=80){
			nearNo++;
		}
	}
	
	//近くのオブジェクトの配列番号を取得
		//target:取得対象のオブジェクト
		private int getNearIObject(IObject[] target) {
			
			int j=0;
			double distance = getDistanceTo(target[j]);
			
			for(int i=1; i<target.length;i++){
				double distance2 = getDistanceTo(target[i]);
				if(distance>distance2){
					distance=distance2;
					j=i;
				}
			}
			
			return j;
		}
	
	//	実動作
	public void move(int lastMoveTime, boolean hitWall, ICar collidedWithCar, ICar hitBySpareTire) {
		
		opponents=getOpponents();
		
		//敵車との距離が100以内⇒プロテクトモード
		protectRange(100);
		
		//他車と衝突
		if(collidedWithCar != null && waitBack == 0 && gasFlag == false){
			//衝突回数によって、バック距離を変える
			if(Collided_count==0){
				//指定範囲内の敵車を攻撃
				if(searchFront(85,20)) attack();
				waitBack = 7;
				Collided_count++;
			}else if(Collided_count==1){
				waitBack = 10;
				Collided_count++;
			}else if(Collided_count==2){
				waitBack = 20;
				Collided_count++;
			}
		}
		
		//壁と衝突
		if(hitWall == true && waitBack == 0 && gasFlag == false){
			waitBack = 2;	
		}
		
		//ガソリン補給
		if(getFuel()<25 && waitBack == 0&& gasFlag == false&&getClockTicks()<400){
			prevNo = getPreviousCheckpoint();
			gasFlag = true;
		}
		
		//前進ー後退
		if(waitBack>0){
			waitBack--;
			int collided_enemy_Angle = getAngle(opponents[getNearIObject(opponents)]);
			if((collided_enemy_Angle < -90 || collided_enemy_Angle> 90 )&& getDistanceTo(opponents[getNearIObject(opponents)]) < 60){
				setThrottle(MAX_THROTTLE);
			}else{
				setSteeringSetting(5);
				setThrottle(MIN_THROTTLE);
			}
		}else if(gasFlag == true){
			//給油処理
			gotoGas(getNearIObject(gasStations));
		}else{
			//前進処理
			int targetNo;
			if(startFlag == true){
				//最寄りのチェックポイントに向かう
				targetNo = nearNo;
				if(prevNo != getPreviousCheckpoint())	startFlag = false;
			}else{
				//次のチェックポイントに向かう		
				targetNo =getPreviousCheckpoint()+1;
			}
			//最後のチェックポイントを通過したら、最初に戻る
			if(targetNo >= checkPoints.length){
				targetNo = 0;
			}
			
			//2回以上、衝突後、給油所に向かうか、別のチェックポイントに向かうかを判断
			if(Collided_count>1){
				if(getFuel()<50){
					//ガソリンが50未満⇒給油所に向かう
					gasFlag=true;
				}else{
					//別のチェックポイントに向かう
					targetNo = minusCheckpoints(getPreviousCheckpoint(),1);
				}
			}
			drive(checkPoints[targetNo]);
		}
	}
	
	//gasStationへ向かう
	//number:一番近いgasStationの配列番号
	private void gotoGas(int number) {
				
		if(getFuel() > 80){
			gasFlag = false;
			startFlag = true;
			Collided_count=0;
			if(getDistanceTo(checkPoints[nearNo])<=80)	nearNo++;
		}else if(getFuel()>35&&getClockTicks()>500){
			gasFlag=false;
			startFlag=true;
			if(getDistanceTo(checkPoints[nearNo])<=80)	nearNo++;
		}else if(checkgotoGas()==false&&Collided_count>=0){
				stop(gasStations[number+1]);
		}else {
			stop(gasStations[number]);
		}
	}
	
	//チェックポイントに向かうか、ガスステーションに向かうかの選択処理
	private boolean checkgotoGas(){
		boolean flag=true;
		int nexttarget = (getPreviousCheckpoint() +1)% checkPoints.length;
		int gasNo = getNearIObject(gasStations);

		if(getDistanceTo(gasStations[gasNo])< getDistanceTo(checkPoints[nexttarget])){
			//ガスステーションよりチェックポイントが遠い場合true
			flag = true;
		}else{
			//ガスステーションよりチェックポイントが近い場合false
			flag = false;
		}
		//ガスステーションに敵車がいた場合false
		for (int i = 0; i < opponents.length; i++){
			if(opponents[i].getDistanceTo(gasStations[gasNo])< 80){
				flag = false; 
			}
		}
		return flag;
	}
	
	//オブジェクトに向かう（停止）
	//target:向かいたいオブジェクト
	private void stop(IObject target) {
		
		int targetAngle = getAngle(target);
		double distance = getDistanceTo(target);
		
		if(distance <25){
			if(isInProtectMode()==false)	enterProtectMode();
			setSteeringSetting(0);
			setThrottle(0);
		}else if(distance<80){
			setSteeringSetting(targetAngle);
			setThrottle(40);
		}else if(distance<200){
			setSteeringSetting(targetAngle);
			setThrottle(60);
		}else {
			drive(target);
		}
	}
	
	//オブジェクトに向かう（通過）
	//target:向かいたいオブジェクト
	private void drive(IObject target) {
		double rate1=0.8,rate2 = 0.9;
		int targetAngle = getAngle(target);
		
		if(targetAngle>10){
			setSteeringSetting(MAX_STEER_RIGHT);
			setThrottle((int)(MAX_THROTTLE*rate2));
		}else if(targetAngle<-10){
			setSteeringSetting(MAX_STEER_LEFT);
			setThrottle((int)(MAX_THROTTLE*rate2));
		}else if(getDistanceTo(target)>30){
			setSteeringSetting(targetAngle/2);
			setThrottle((int)(MAX_THROTTLE));
		}else{
			setSteeringSetting(targetAngle);
			setThrottle((int)(MAX_THROTTLE*rate1));
		}
	}

	//角度を0～360から-180～180に変換
	//angle:変換対象
	private int changeAngle(int angle) {

		if(180 < angle){
			angle = angle - 360;
		}
		if(angle < -180){
			angle = angle + 360;
		}
		return angle;
	}
	
	//対象への相対角度を取得
		//target:対象のオブジェクト
		private int getAngle(IObject target){
			
			int targetAngle = getHeadingTo(target)-getHeading();
			
			return changeAngle(targetAngle);
			
		}

	//タイヤを飛ばす
	private void attack() {
		if(isReadyToThrowSpareTire()){
			throwSpareTire();
		}
	}
	
	//チェックポイントの番号を一つ減らす
	//current_number:初期の番号
	private int minusCheckpoints(int current_number){
			
		int number = current_number - 1;
	
		if (number == 0){
			number = checkPoints.length - 1;
		}
			
		return number;
			
	}
	
	//チェックポイントの番号を複数回減らす
	//current_number:初期の番号, count:増やしたい回数
	private int minusCheckpoints(int current_number , int count){
			
		int number = 0;
			
		for(int i=0;i<count;i++){
				
			if(i==0)
				number = minusCheckpoints(current_number);
			else
				number = minusCheckpoints(number);
		}
			
		return number;
	}

	//前方に他チームの車が存在するか
		//distance:距離 angle:角度
		private boolean searchFront(double distance,int angle){
			
			for(int i=0;i<opponents.length;i++){
				if(searchVicinity(distance))
					if(Math.abs(getAngle(opponents[i]))<angle)
							return true;
			}
			
			return false;
			
		}

	//周囲に他チームの車が存在するか(存在する⇒true)
	//distance:距離
	private boolean searchVicinity(double distance){
		
		for(int i=0;i<opponents.length;i++){
			if(getDistanceTo(opponents[i])<distance)
					if(!teamCheck(opponents[i]))
						return true;
		}
		
		return false;
		
	}

	//他車がどれだけ近づいた際にプロテクトモードに入るか
	//protect_range:範囲の大きさ
	private void protectRange(double protect_range){
	
		for(int i=0;i<opponents.length;i++)
			if(searchVicinity(protect_range))
				if(isInProtectMode() == false)
					enterProtectMode();

	}

	//敵か味方かの判定（味方ならtrue,敵ならfalseを返す）
	//target:判別したい対象
	private boolean teamCheck(ICar target){
		
		if(getSchoolName().equals(target.getSchoolName()))
			return true;
		else
			return false;
		
	}
	
}