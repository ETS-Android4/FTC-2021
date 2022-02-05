package org.firstinspires.ftc.teamcode.TFODOHM.TFTesting;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.TFODOHM.ODMain.TFODModule;
import org.firstinspires.ftc.teamcode.TFODOHM.TFMaths.TFMathExtension;
import org.firstinspires.ftc.teamcode.TFODOHM.TFMaths.Vector2f;
import org.firstinspires.ftc.teamcode.TFODOHM.TFMaths.Vector3f;
import org.firstinspires.ftc.teamcode.momm.MultiOpModeManager;
import org.firstinspires.ftc.teamcode.robot.NewNewDrive;
import org.firstinspires.ftc.teamcode.robot.tfMathArcTest;
import org.firstinspires.ftc.teamcode.utils.OrderedEnum;
import org.firstinspires.ftc.teamcode.utils.OrderedEnumHelper;

import kotlin.jvm.internal.Reflection;

@Config
@Autonomous(name = "TFDrive", group = "Test")
public class TFDriveTest extends OpMode {


    private TFODModule tfodModule;
    private NewNewDrive drive;

    private AUTO_STATE state = AUTO_STATE.DONE;

    @Override
    public void init() {
        try {
            tfodModule = new TFODModule();
            tfodModule.init();
        } catch (Exception e ){
            telemetry.log().add(tfodModule.getClass().getSimpleName() + " is not initializing.");
        }

        try {
            drive = new NewNewDrive();
            drive.init();
        } catch (Exception e ){
            telemetry.log().add(drive.getClass().getSimpleName() + " is not initializing.");
        }

        tfodModule.init();

        telemetry.addData("TFOD Null? ", tfodModule == null ? "Yes" : "No");
        telemetry.addData("TFObjectDetection Null? ", tfodModule.getTfod() == null ? "Yes" : "No");
        telemetry.addData("Vuforia Null? ", tfodModule.getVuforia() == null ? "Yes" : "No");
    }

    @Override
    public void init_loop() {

    }

    @Override
    public void start() {
        super.start();
        drive.setDoneFalse();
        state = AUTO_STATE.VERIFICATION;
    }

    private boolean scanned = false, sorted = false, calculated = false, startedMove = false, reversingMove = false;
    private Vector2f tempV2, target;
    private Vector3f targetPreCasted;
    private double storedRadius, storedArcLength;
    private boolean inIMG = false;

    public static double speedMin = 0.1;
    public static double speedMax = 0.5;
    @Override
    public void loop() {
        switch (state){
            case VERIFICATION:
                inIMG = tfodModule.verifyImg();
                if (inIMG == true){
                    state = AUTO_STATE.SCAN;
                }
                break;

            case SCAN: //scan for objects
                if (!tfodModule.isBusy() && (scanned == false)) {
                    tfodModule.scan();
                    scanned = true;
                }
                if (!tfodModule.isBusy() && (sorted == false)){
                    tempV2 = tfodModule.sortCBBB();
                    sorted = true;
                }
                if (!tfodModule.isBusy() && (calculated == false)){
                    targetPreCasted = tfodModule.calcCoordinate(tempV2);
                    calculated = true;
                }
                if (!tfodModule.isBusy() && (scanned && sorted && calculated)){
                    state = AUTO_STATE.RESET_VAR;
                }
                break;

            case RESET_VAR: //reset all variables used to check stuff in the previous case
                scanned = false;
                sorted = false;
                calculated = false;
                startedMove = false;
                reversingMove = false;
                target = new Vector2f(targetPreCasted.getX(), targetPreCasted.getZ());
                double[] f = TFMathExtension.makeArcV1(target);
                storedRadius = f[0];
                storedArcLength = f[1];
                inIMG = false;
                state = AUTO_STATE.START_MOVE;
                break;

            case START_MOVE:
                if (startedMove == false){
                    startedMove = true;
                    drive.arcTo(storedRadius, storedArcLength, speedMin, speedMax);
                }
                if (startedMove == true && (!drive.isBusy() && drive.isDone())){
                    startedMove = false;
                    state = AUTO_STATE.REVERSE_MOVE;
                }
                break;

            case REVERSE_MOVE:
                if (reversingMove = false){
                    reversingMove = true;
                    drive.arcTo(-storedRadius, -storedArcLength, speedMin, speedMax);
                }
                if (reversingMove && (!drive.isBusy() && drive.isDone())){
                    reversingMove = false;
                    state = AUTO_STATE.VERIFICATION;
                }
                break;

            case DONE:
                break;
        }
        telemetry.addData("Current State: ", state);
        telemetry.addData("Calculated Vector: ", target);
        telemetry.addData("Scanned: ", scanned);
        telemetry.addData("Sorted: ", sorted);
        telemetry.addData("Calculated: ", calculated);
        telemetry.addData("StartedMove: ", startedMove);
        telemetry.addData("ReversingMove: ", reversingMove);
    }

    @Override
    public void stop() {
        tfodModule.stop();
        drive.stop();
        state = AUTO_STATE.DONE;
    }

    enum AUTO_STATE implements OrderedEnum {
        VERIFICATION,
        SCAN,
        RESET_VAR,
        START_MOVE,
        REVERSE_MOVE,
        DONE;
        public TFDriveTest.AUTO_STATE next() {
            return OrderedEnumHelper.next(this);
        }
    }
}
