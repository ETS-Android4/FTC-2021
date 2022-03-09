package org.firstinspires.ftc.teamcode.robot;


import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.utils.LoopUtil;

@Config
@Autonomous(name = "Loop Util Testing", group = "Test")
public class LoopUtilTesting extends LoopUtil {
    private String log_title = "";
    private int opFixedUpdateCount = 0;

    @Override
    public void opInit() {
        log_title = "LoopUtilTesting Log: ";
    }

    @Override
    public void opInitLoop() {

    }

    @Override
    public void opStart() {

    }

    @Override
    public void opUpdate(double deltaTime) {
        update_wait_for(10);
        telemetry.addData("Hello World! Current Time: ", getCurrentTime());
    }

    @Override
    public void opFixedUpdate(double deltaTime) {
        opFixedUpdateCount++;
        telemetry.addData("Fixed Update Count: ", opFixedUpdateCount);
        telemetry.addData("Delta Time: ", deltaTime);
    }

    @Override
    public void opStop() {

    }
}
