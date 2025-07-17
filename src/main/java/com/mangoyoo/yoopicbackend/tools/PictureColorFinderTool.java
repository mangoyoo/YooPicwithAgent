package com.mangoyoo.yoopicbackend.tools;



import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mangoyoo.yoopicbackend.service.PictureService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PictureColorFinderTool {

    @Resource
    @Lazy
    private PictureService pictureService;

    @Tool(description = "当且仅当用户明确说了要用本站的图片的时候才能调用这个工具，这个工具的作用是: Find pictures(only in this site,not from Internet) by color similarity from public gallery, and return URLs as a string.")
    public String findPicturesByColor (
            @ToolParam(description = "Target color in hex format (e.g., #FF0000 for red)") String picColor,
            @ToolParam(description = "Number of pictures to find ") Integer count, @ToolParam(description = "A summary of completed steps and explanation of the next steps in Chinese") String summary) {

        try {
            // 1. 参数验证
            if (StrUtil.isBlank(picColor)) {
                return "Error: Color parameter cannot be empty";
            }

            // 验证颜色格式
            String validationResult = validateColorFormat(picColor);
            if (!validationResult.equals("valid")) {
                return validationResult;
            }

            // 设置默认数量
            if (count == null || count <= 0) {
                count = 1;
            }


            log.info("开始按颜色查找图片，颜色: {}, 数量: {}", picColor, count);

            // 2. 调用服务查找图片
            List<String> foundUrls = pictureService.findPictureUrlsByColorSimilarity(picColor, count);

            if (CollUtil.isEmpty(foundUrls)) {
                return "No pictures found with similar color";
            }

            // 3. 将URL组合成字符串返回
            String result = String.join(",", foundUrls);
            log.info("成功找到{}张相似颜色图片", foundUrls.size());

            return result;

        } catch (Exception e) {
            log.error("按颜色查找图片失败", e);
            return "Error finding pictures by color: " + e.getMessage();
        }
    }

//    @Tool(description = "Find pictures by color similarity with default count (12 pictures).")
//    public String findPicturesByColorDefault(
//            @ToolParam(description = "Target color in hex format (e.g., #FF0000 for red)") String picColor) {
//        return findPicturesByColor(picColor, 12);
//    }

    /**
     * 验证颜色格式
     */
    private String validateColorFormat(String picColor) {
        if (StrUtil.isBlank(picColor)) {
            return "Error: Color cannot be empty";
        }

        // 确保颜色以#开头
        if (!picColor.startsWith("#")) {
            // 尝试自动添加#
            picColor = "#" + picColor;
        }

        // 验证长度（#RRGGBB = 7位 或 #RGB = 4位）
        if (picColor.length() != 7 && picColor.length() != 4) {
            return "Error: Color format should be #RRGGBB or #RGB";
        }

        // 验证是否为有效的十六进制
        String hexPart = picColor.substring(1);
        try {
            Integer.parseInt(hexPart, 16);
        } catch (NumberFormatException e) {
            return "Error: Invalid hex color format. Use #RRGGBB (e.g., #FF0000)";
        }

        // 如果是3位格式，转换为6位
        if (picColor.length() == 4) {
            StringBuilder fullColor = new StringBuilder("#");
            for (int i = 1; i < picColor.length(); i++) {
                char c = picColor.charAt(i);
                fullColor.append(c).append(c);
            }
            picColor = fullColor.toString();
        }

        return "valid";
    }

    /**
     * 批量按颜色查找图片（返回URL列表）
     */
    public List<String> findPicturesByColorBatch(String picColor, Integer count) {
        try {
            log.info("批量按颜色查找图片，颜色: {}, 数量: {}", picColor, count);

            if (count == null || count <= 0) {
                count = 12;
            }

            return pictureService.findPictureUrlsByColorSimilarity(picColor, count);
        } catch (Exception e) {
            log.error("批量按颜色查找图片失败", e);
            return List.of();
        }
    }

    /**
     * 验证查找参数
     */
    public String validateColorSearchParams(String picColor, Integer count) {
        String colorValidation = validateColorFormat(picColor);
        if (!colorValidation.equals("valid")) {
            return colorValidation;
        }

        if (count != null) {
            if (count <= 0) {
                return "Error: Count must be a positive number";
            }

            if (count > 50) {
                return "Warning: Large count may impact performance (max recommended: 50)";
            }
        }

        return "Parameters validation passed";
    }

    /**
     * 获取推荐的颜色值示例
     */
    public String getColorExamples() {
        return "Color examples: #FF0000 (red), #00FF00 (green), #0000FF (blue), " +
                "#FFFF00 (yellow), #FF00FF (magenta), #00FFFF (cyan), " +
                "#FFA500 (orange), #800080 (purple), #FFC0CB (pink), #A52A2A (brown)";
    }

    /**
     * 标准化颜色格式
     */
    private String normalizeColor(String picColor) {
        if (StrUtil.isBlank(picColor)) {
            return null;
        }

        // 移除空格并转大写
        picColor = picColor.trim().toUpperCase();

        // 确保以#开头
        if (!picColor.startsWith("#")) {
            picColor = "#" + picColor;
        }

        // 如果是3位格式，转换为6位
        if (picColor.length() == 4) {
            StringBuilder fullColor = new StringBuilder("#");
            for (int i = 1; i < picColor.length(); i++) {
                char c = picColor.charAt(i);
                fullColor.append(c).append(c);
            }
            picColor = fullColor.toString();
        }

        return picColor;
    }

    /**
     * 根据颜色名称获取十六进制值（可选功能）
     */
    public String getColorByName(String colorName) {
        if (StrUtil.isBlank(colorName)) {
            return "Error: Color name cannot be empty";
        }

        String name = colorName.toLowerCase().trim();
        return switch (name) {
            case "red" -> "#FF0000";
            case "green" -> "#00FF00";
            case "blue" -> "#0000FF";
            case "yellow" -> "#FFFF00";
            case "magenta", "pink" -> "#FF00FF";
            case "cyan" -> "#00FFFF";
            case "orange" -> "#FFA500";
            case "purple" -> "#800080";
            case "brown" -> "#A52A2A";
            case "black" -> "#000000";
            case "white" -> "#FFFFFF";
            case "gray", "grey" -> "#808080";
            default -> "Unknown color name. Use hex format like #FF0000";
        };
    }
}

