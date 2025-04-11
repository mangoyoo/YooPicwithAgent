package com.mangoyoo.yoopicbackend.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 终止工具（作用是让自主规划智能体能够合理地中断）
 */
@Component
public class TerminateTool {

    @Tool(description = """
            It can stop the interaction.
            For shut down the conversation in any time.
            If no further action is required,  invoke this tool to shut down the conversation  NOW!!! 
            """)
    public String doTerminate(@ToolParam(description = " If the task result has url you must put it int there. Final result of the task.") String finalResult) {
        return "任务结束: " + finalResult;
    }
}

