package org.firstinspires.ftc.teamcode.swerve;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.RobotLog;
import com.sun.tools.javac.util.ArrayUtils;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.sensors.pot.DoublePotentiometer;
import org.firstinspires.ftc.teamcode.utils.Polynomial;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class SwervePod extends PID<Double>{

    //tuning constants, 0 == not used
    private final static double Kp = 0.2;
    private final static double Ki = 0;
    private final static double Kd = 0;

    //hardware
    private final DcMotor m1, m2;
    private DoublePotentiometer pot;

    //targets
    private double targetVelocity, targetAngle;

    //variable output
    private Telemetry telemetry;


    //initialization and hardware setup
    public SwervePod(Telemetry telemetry, DcMotor m1, DcMotor m2, DoublePotentiometer pot) {
        super(telemetry, Kp, Ki, Kd);
        this.telemetry = telemetry;
        this.m1 = m1;
        this.m2 = m2;
        this.pot = pot;
        targetAngle = 0;
        targetVelocity = 0;

        m1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        m2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        m1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        m2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        m1.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        m2.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

    }

    //zero out the wheels
    public void tune(){

        m2.setTargetPosition(0);
        m2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        m2.setPower(0.5);
        m1.setPower(0.2);

        int spinRes = 5; // do it 5 times to find the center of each zero

        int lowerBound = -283;
        int[] zeros = new int[spinRes];
        int[] points = new int[spinRes];

        boolean cool = true;
        for(int i = 0; i < spinRes;){

            //use the offset potentiometer values to count revolutions
            if(pot.isZeroOne()) {
                int pos = m1.getCurrentPosition();
                if (lowerBound == -283) lowerBound = pos;

                zeros[i] += (pos - lowerBound);
                points[i] ++;

                cool = true;
            }
            if(pot.isZeroTwo() && cool){
                i++;
                cool = false;
            }
            //visualization
            telemetry.addData("pot1", pot.getMV1());
            telemetry.addData("pot2", pot.getMV2());
            telemetry.update();
        }

        //calculate motor encoder ticks per revolution based on data
        float tpr = 0;
        for(int i = 0;i < spinRes; i++) {
            if(points[i] != 0) tpr += (zeros[i] / points[i]);
            else spinRes--;
        }
        tpr /= spinRes;
        tpr /= 2;

        m1.setPower(0);

        RobotLog.d("AUTO TUNE tpr: " + tpr);

        m1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        m1.setTargetPosition((int)tpr);
        m1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        m1.setPower(0.2);

        ArrayList<Double> pd1 = new ArrayList<Double>();
        ArrayList<Double> pd2 = new ArrayList<Double>();
        ArrayList<Double> td1 = new ArrayList<Double>();
        ArrayList<Double> td2 = new ArrayList<Double>();

        double tpd = tpr/360;

        for(;m1.getCurrentPosition() < tpr;){
            telemetry.addData("pot1",m1.getCurrentPosition()/tpd);
            telemetry.addData("pot2",0);
            if(pot.getMV1() > 0.2 && pot.getMV1() < 3.1){
                pd1.add(pot.getMV1());
                td1.add(m1.getCurrentPosition()/tpd);
                //telemetry.addData("pot1",1);
            }
            if(pot.getMV2()>0.15&&pot.getMV2()<2.7){
                pd2.add(pot.getMV2());
                td2.add(m1.getCurrentPosition()/tpd);
                telemetry.addData("pot2",1);
            }
            telemetry.update();
        }

        double[] pd1a = new double[pd1.size()];
        double[] td1a = new double[pd1.size()];

        double[] pd2a = new double[pd2.size()];
        double[] td2a = new double[pd2.size()];

        for(int i = 0; i < pd1a.length; i++){
            pd1a[i] = (double)pd1.get(i);
            td1a[i] = (double)td1.get(i);
        }
        for(int i = 0; i < pd2a.length; i++){
            pd2a[i] = (double)pd2.get(i);
            td2a[i] = (double)td2.get(i);
        }

        try {
            RobotLog.d("AUTOTUNE poly 1: " + Arrays.toString(Polynomial.getConstants(pd1a, td1a,20)));
            RobotLog.d("AUTOTUNE poly 2: " + Arrays.toString(Polynomial.getConstants(pd2a, td2a,20)));
        } catch (Exception e) {
            RobotLog.d("uh oh " + e.getMessage() + " sdfsd" + Arrays.toString(e.getStackTrace()));
        }


        //reset hardware
        m1.setPower(0);
        m2.setPower(0);

        m1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        m2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        m1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        m2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);


    }

    public void loop() {
        //get current position and set that in out correction
        setFeedBack(-(((double)m1.getCurrentPosition()/277)+((double)m2.getCurrentPosition()/277)));//pot.getAngleD());

        //visualization
        telemetry.addData("feedBack", -(((double)m1.getCurrentPosition()/277)+((double)m2.getCurrentPosition()/277)));

        // do fancy math and calculate correction value based on the error of the angle
        double correction = PIDloop();

        //output and visualization for each side
        m1.setPower(targetVelocity + correction);
        telemetry.addData("mot1", targetVelocity + correction);

        m2.setPower(-targetVelocity + correction);
        telemetry.addData("mot2", -targetVelocity + correction);
    }

    //setter for velocity target
    public void setTargetVelocity(double velocity){
        targetVelocity = velocity;
    }

    //setter for angle target
    public void setTargetAngle(double angle){
        targetAngle = angle;
        setSetPoint(angle);
    }

    public void hmmm(int dij){
        m1.setTargetPosition(dij);
        m2.setTargetPosition(0);
        m1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        m2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        m1.setPower(0.5);
        m2.setPower(0.5);
    }

    public void hmmmm(){
        m1.setPower(-1);
        m2.setPower(1);
    }

    //spins around - mainly for testing
    public void spin(){
        m1.setPower(0.2);
        m2.setPower(0);

        telemetry.addData("pot1", pot.getAngleD());
        telemetry.addData("pot2", pot.getAngleD());
        telemetry.addData("potT", pot.getAngleD());
        telemetry.update();
    }

    //stop
    public void brake(){
        m1.setPower(0);
        m2.setPower(0);
    }

    //when zeros out the wheel as best its able while running
    public void zero(){
        if(pot.isZeroOne() && (Math.abs(m1.getCurrentPosition()) > 10 || Math.abs(m2.getCurrentPosition()) > 10)){
            m1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            m2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        } else {
            if(pot.getMV1() > pot.getMV2()) targetAngle += 0.01;
            else targetAngle -= 0.01;
        }
    }

}