package org.exh.nianhuawechatminiprogrambackend.util;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.entity.Seat;
import org.exh.nianhuawechatminiprogrambackend.mapper.SeatMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * 座位生成测试工具
 *
 * 功能说明：
 * - 为session_id为1和2的两个场次生成测试座位数据
 * - 每个场次生成90个座位
 * - 座位布局：18桌，每桌5人
 * - 前30座位（0-29）：前排，价格30000分（300元）
 * - 中30座位（30-59）：中排，价格20000分（200元）
 * - 后30座位（60-89）：后排，价格10000分（100元）
 * - 桌号：A-R 共18个桌
 * - 生成数据：180个座位记录
 *
 * 使用方法：
 * 1. 确保数据库连接配置正确
 * 2. 执行前清空seats表：TRUNCATE TABLE seats;
 * 3. 运行本测试类（JUnit测试）
 * 4. 查看session_id为1和2的座位数据
 */
@Slf4j
@SpringBootTest
public class SeatGeneratorTest {

    @Autowired
    private SeatMapper seatMapper;

    // 每个场次的座位数
    private static final int SEATS_PER_SESSION = 90;

    // 每桌人数
    private static final int PEOPLE_PER_TABLE = 5;

    // 桌号字母数组（18桌）
    private static final String[] TABLE_LETTERS = {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
        "K", "L", "M", "N", "O", "P", "Q", "R"
    };

    // 价格（单位：分）
    private static final long FRONT_PRICE = 30000;  // 前排 300元
    private static final long MIDDLE_PRICE = 20000; // 中排 200元
    private static final long BACK_PRICE = 10000;   // 后排 100元

    // 座位类型
    private static final String FRONT_TYPE = "front";
    private static final String MIDDLE_TYPE = "middle";
    private static final String BACK_TYPE = "back";

    /**
     * 测试：生成座位数据
     */
    @Test
    public void generateSeatsTest() {
        log.info("=== 开始生成座位数据 ===");

        // 清空历史数据
        log.info("清空seats表历史数据...");
        int deletedCount = seatMapper.delete(null);
        log.info("已删除{}条历史数据", deletedCount);

        // 生成座位数据
        List<Seat> seatList = generateSeats();

        // 批量插入数据库（使用循环插入）
        if (!seatList.isEmpty()) {
            log.info("开始插入座位数据到数据库，共{}条记录", seatList.size());

            int successCount = 0;
            for (Seat seat : seatList) {
                int rows = seatMapper.insert(seat);
                if (rows > 0) {
                    successCount++;
                }
            }

            log.info("座位数据插入完成！成功插入{}条记录", successCount);

            // 验证数据
            verifySeats();
        }

        log.info("=== 座位生成完成 ===");
    }

    /**
     * 生成座位数据
     */
    private List<Seat> generateSeats() {
        List<Seat> seatList = new ArrayList<>();

        for (Long sessionId : new Long[]{1L, 2L}) {
            log.info("开始生成session_id={}的座位数据", sessionId);

            for (int i = 0; i < SEATS_PER_SESSION; i++) {
                Seat seat = createSeat(sessionId, i);
                seatList.add(seat);
            }

            log.info("session_id={}座位生成完成，共{}个", sessionId, SEATS_PER_SESSION);
        }

        return seatList;
    }

    private Seat createSeat(Long sessionId, int seatIndex) {
        Seat seat = new Seat();
        seat.setSessionId(sessionId);
        seat.setStatus(0);

        int tableIndex = seatIndex / PEOPLE_PER_TABLE;
        int seatNumber = (seatIndex % PEOPLE_PER_TABLE) + 1;
        String tableLetter = TABLE_LETTERS[tableIndex];

        seat.setSeatId(tableLetter + seatNumber);
        seat.setSeatName(tableLetter + "桌-" + seatNumber + "号");

        if (seatIndex < 30) {
            seat.setSeatType(FRONT_TYPE);
            seat.setPrice(FRONT_PRICE);
        } else if (seatIndex < 60) {
            seat.setSeatType(MIDDLE_TYPE);
            seat.setPrice(MIDDLE_PRICE);
        } else {
            seat.setSeatType(BACK_TYPE);
            seat.setPrice(BACK_PRICE);
        }

        return seat;
    }

    private void verifySeats() {
        log.info("\n=== 验证座位数据 ===");

        for (Long sessionId : new Long[]{1L, 2L}) {
            log.info("\n--- Session {} 统计 ---", sessionId);

            long frontCount = seatMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Seat>()
                            .eq(Seat::getSessionId, sessionId)
                            .eq(Seat::getSeatType, FRONT_TYPE)
            );
            log.info("前排座位数: {}", frontCount);

            long middleCount = seatMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Seat>()
                            .eq(Seat::getSessionId, sessionId)
                            .eq(Seat::getSeatType, MIDDLE_TYPE)
            );
            log.info("中排座位数: {}", middleCount);

            long backCount = seatMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Seat>()
                            .eq(Seat::getSessionId, sessionId)
                            .eq(Seat::getSeatType, BACK_TYPE)
            );
            log.info("后排座位数: {}", backCount);
        }

        log.info("\n=== 价格 ===");
        log.info("前排: {}分 ({}元)", FRONT_PRICE, FRONT_PRICE / 100);
        log.info("中排: {}分 ({}元)", MIDDLE_PRICE, MIDDLE_PRICE / 100);
        log.info("后排: {}分 ({}元)", BACK_PRICE, BACK_PRICE / 100);
    }
}
