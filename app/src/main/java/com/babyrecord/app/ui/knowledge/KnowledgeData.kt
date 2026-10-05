package com.babyrecord.app.ui.knowledge

data class KnowledgeSection(val heading: String, val body: List<String>)

data class KnowledgeArticle(
    val id: Int,
    val title: String,
    val ageMinMonths: Int,
    val ageMaxMonths: Int,
    val category: String,
    val emoji: String,
    val sections: List<KnowledgeSection>
)

/**
 * 育儿百科内容（离线内置）。
 * 内容综合参考：国家卫生健康委员会儿童保健相关规范、中国营养学会《中国居民膳食指南（2022）》婴幼儿喂养部分、
 * 世界卫生组织（WHO）婴幼儿养育建议，仅供家庭日常参考，不能替代医生诊断；宝宝有异常请及时就医。
 */
object KnowledgeData {

    val articles: List<KnowledgeArticle> = listOf(
        KnowledgeArticle(
            id = 1,
            title = "新生儿期：喂养与日常护理",
            ageMinMonths = 0, ageMaxMonths = 1,
            category = "喂养", emoji = "🍼",
            sections = listOf(
                KnowledgeSection("按需喂养", listOf(
                    "母乳喂养一般每 24 小时 8~12 次，宝宝饿了（转头张口、吃手）就喂，不必严格掐时间。",
                    "配方奶喂养参考：新生儿每次约 30~60ml，逐渐增加；两次奶之间可喂少量水的情况较少，一般不需要额外补水。"
                )),
                KnowledgeSection("睡眠特点", listOf(
                    "新生儿每天睡 16~20 小时，没有昼夜概念，夜里醒 2~3 次很正常。",
                    "仰卧睡最安全；床上不要放松软的枕头、毛绒玩具，降低窒息风险。"
                )),
                KnowledgeSection("日常观察", listOf(
                    "生理性黄疸一般出生后 2~3 天出现、2 周内消退；若加重、退而复现或精神差，及时就医。",
                    "脐带残端保持干燥，自然脱落，不要用力抠。"
                ))
            )
        ),
        KnowledgeArticle(
            id = 2,
            title = "1~3月：建立规律与早期互动",
            ageMinMonths = 1, ageMaxMonths = 3,
            category = "发育", emoji = "🧸",
            sections = listOf(
                KnowledgeSection("喂养", listOf(
                    "仍以按需哺乳为主，多数宝宝会形成 2.5~3 小时左右的节奏。",
                    "出生后数日起每日补充维生素 D 400IU（遵医嘱），有助钙吸收。"
                )),
                KnowledgeSection("互动与运动", listOf(
                    "宝宝开始追视、追听：用黑白卡或红色玩具在眼前 20~30cm 缓慢移动。",
                    "每天数次俯卧练习（tummy time），每次 3~5 分钟，锻炼抬头。"
                )),
                KnowledgeSection("睡眠", listOf(
                    "昼夜节律开始形成：白天小睡 3~4 次，夜里最长一觉可达 4~5 小时。",
                    "夜里醒来喂奶尽量保持安静、昏暗，帮助宝宝分清昼夜。"
                ))
            )
        ),
        KnowledgeArticle(
            id = 3,
            title = "4~6月：辅食添加准备与开吃",
            ageMinMonths = 4, ageMaxMonths = 6,
            category = "喂养", emoji = "🥣",
            sections = listOf(
                KnowledgeSection("辅食信号", listOf(
                    "能扶坐、挺舌反射消失、对大人吃饭表现出兴趣，通常满 6 月龄左右就绪。",
                    "世界卫生组织建议满 6 月龄（180 天）开始添加辅食，同时继续母乳或配方奶。"
                )),
                KnowledgeSection("第一口辅食", listOf(
                    "首选强化铁的婴儿米粉，从 1~2 勺尝试，观察 2~3 天有无皮疹、腹泻等过敏表现。",
                    "6 月龄后宝宝对铁的需求高，红肉泥、肝泥也是很好的铁来源。"
                )),
                KnowledgeSection("大运动", listOf(
                    "多数宝宝 6 月龄左右能独坐片刻、会翻身；多给安全的地面练习空间。"
                ))
            )
        ),
        KnowledgeArticle(
            id = 4,
            title = "7~9月：咀嚼进阶与爬行准备",
            ageMinMonths = 7, ageMaxMonths = 9,
            category = "喂养", emoji = "🥕",
            sections = listOf(
                KnowledgeSection("食物质地", listOf(
                    "从细腻泥糊过渡到带小颗粒的稠泥、碎末，锻炼咀嚼；可以给磨牙饼干等手指食物。",
                    "每天奶量保持 600ml 以上，辅食 2 次左右，蛋黄、肉泥、豆腐、蔬果逐步丰富。"
                )),
                KnowledgeSection("大运动与认知", listOf(
                    "独坐越来越稳，开始腹爬/手膝爬；在地上铺爬行垫，鼓励自由探索。",
                    "喜欢藏猫猫游戏，是客体永久性发展的表现。"
                )),
                KnowledgeSection("睡眠", listOf(
                    "白天 2 次小睡较常见；夜醒可能与出牙、大运动飞跃有关，尽量保持原有安抚方式。"
                ))
            )
        ),
        KnowledgeArticle(
            id = 5,
            title = "10~12月：自主进食萌芽",
            ageMinMonths = 10, ageMaxMonths = 12,
            category = "喂养", emoji = "🍚",
            sections = listOf(
                KnowledgeSection("吃饭", listOf(
                    "提供小块软水果、蒸软的蔬菜条，让宝宝抓着吃（注意整颗坚果、果冻等窒息风险食物）。",
                    "辅食可到 2~3 次/日，奶量约 600ml；1 岁内辅食不加盐、糖和蜂蜜。"
                )),
                KnowledgeSection("语言与情感", listOf(
                    "会有意识地叫爸爸妈妈，能听懂简单指令；多和宝宝说话、读布书。",
                    "认生、分离焦虑是这个阶段的正常表现，离开前打招呼、仪式感固定，能减轻焦虑。"
                ))
            )
        ),
        KnowledgeArticle(
            id = 6,
            title = "1岁~1岁半：向家庭饮食过渡",
            ageMinMonths = 12, ageMaxMonths = 18,
            category = "喂养", emoji = "🥦",
            sections = listOf(
                KnowledgeSection("饮食结构", listOf(
                    "一日三餐＋2 次加餐，食物逐渐接近家庭口味，但仍要少盐少糖、单独清淡烹制。",
                    "奶量约 400~500ml/日，可用吸管杯/敞口杯喝奶，慢慢戒奶瓶。"
                )),
                KnowledgeSection("大运动与疫苗", listOf(
                    "独立行走是这个阶段的大事，鞋底要软、场地要安全。",
                    "18 月龄前后有百白破第 4 剂、麻腮风第 2 剂、甲肝疫苗等接种安排，记得看疫苗接种本。"
                ))
            )
        ),
        KnowledgeArticle(
            id = 7,
            title = "1岁半~2岁：语言爆发与如厕信号",
            ageMinMonths = 18, ageMaxMonths = 24,
            category = "发育", emoji = "💬",
            sections = listOf(
                KnowledgeSection("语言", listOf(
                    "词汇量快速增长，会说短句；少纠正、多回应，用完整的句子复述宝宝的话。",
                    "每天亲子共读 10~20 分钟，是性价比最高的早教。"
                )),
                KnowledgeSection("如厕训练", listOf(
                    "出现尿布能保持干燥 2 小时、对坐便器感兴趣等信号再开始，通常 18~30 月龄之间。",
                    "不强迫、不批评，白天先练、夜尿以后再说。"
                )),
                KnowledgeSection("睡眠", listOf(
                    "全天睡眠约 11~14 小时，多数宝宝中午保留 1 次午睡即可。"
                ))
            )
        ),
        KnowledgeArticle(
            id = 8,
            title = "2~3岁：习惯养成与视力保护",
            ageMinMonths = 24, ageMaxMonths = 36,
            category = "健康", emoji = "🌳",
            sections = listOf(
                KnowledgeSection("生活习惯", listOf(
                    "每天户外活动 2 小时以上，跑跳攀爬都有利于大运动和视力发育。",
                    "2 岁以内避免接触电子屏幕；2 岁以上单次不超过 20 分钟。"
                )),
                KnowledgeSection("规则与情绪", listOf(
                    "自我意识萌发、爱说不要，用有限选择（穿红衣还是蓝衣？）代替硬碰硬。",
                    "发脾气温和陪在身边，等平静后再简单讲道理。"
                ))
            )
        ),
        KnowledgeArticle(
            id = 9,
            title = "发热处理与疫苗后护理常识",
            ageMinMonths = 0, ageMaxMonths = 36,
            category = "健康", emoji = "🌡️",
            sections = listOf(
                KnowledgeSection("发热怎么办", listOf(
                    "3 月龄以内宝宝发热（≥38℃）属急症，请立即就医，不要自行捂汗。",
                    "6 月龄以上精神状态好、能吃能玩，可先物理降温观察；用药请遵医嘱，避免交替叠加退烧药。"
                )),
                KnowledgeSection("疫苗接种后", listOf(
                    "接种当天针眼 24 小时内不沾水；低热、局部红肿一般 1~2 天自行消退。",
                    "出现持续高热、精神萎靡、大面积皮疹，及时联系接种门诊或就医。"
                ))
            )
        ),
        KnowledgeArticle(
            id = 10,
            title = "居家安全与异物窒息急救",
            ageMinMonths = 0, ageMaxMonths = 36,
            category = "安全", emoji = "🛡️",
            sections = listOf(
                KnowledgeSection("居家排查", listOf(
                    "小零件、纽扣电池、药品、清洁剂放到宝宝够不到的地方；桌角装防撞条、插座加保护盖。",
                    "热水壶、汤锅放在宝宝抓不到的位置；洗澡水先放冷水再放热水，水温 37~38℃。"
                )),
                KnowledgeSection("异物卡喉", listOf(
                    "1 岁以内：脸朝下趴在大人前臂、头低于躯干，肩胛骨之间拍背 5 次，再翻转做 5 次压胸，交替进行。",
                    "1 岁以上：海姆立克腹部冲击法。建议家长提前观看权威急救教学视频并练习。"
                ))
            )
        )
    )

    fun byId(id: Int): KnowledgeArticle? = articles.firstOrNull { it.id == id }

    fun ageLabel(months: Int): String = when {
        months < 1 -> "0月龄"
        months < 12 -> "${months}月龄"
        else -> "${months / 12}岁${if (months % 12 > 0) "${months % 12}个月" else ""}"
    }
}
