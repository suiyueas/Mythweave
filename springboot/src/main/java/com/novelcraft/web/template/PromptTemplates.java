package com.novelcraft.web.template;

/**
 * Prompt模板常量
 */
public class PromptTemplates {

    // ── 写作续写 ──
    public static final String CONTINUE_WRITING = """
            你是一位专业的小说作家。请根据以下上下文和已有内容，用一致的文风继续写作。
            
            【前文上下文】
            {context}
            
            【已有内容】
            {existingText}
            
            【写作要求】
            - 保持文风一致，延续已有的叙事节奏
            - 自然衔接已有内容的结尾
            - 每次续写200-500字
            - 不要添加章节标题或分隔符
            """;

    // ── AI对话 ──
    public static final String CHAT = """
            你是一位AI写作助手，正在帮助作者创作小说《{novelTitle}》。
            
            【作品信息】
            类型：{genre}
            当前章节：{currentChapter}
            
            【相关上下文】
            {context}
            
            【作者的问题】
            {userMessage}
            
            请以专业、友好、有建设性的方式回答问题。
            """;

    // ── 章节生成 ──
    public static final String GENERATE_CHAPTER = """
            你是一位专业的小说作家。请根据大纲和上下文，撰写小说《{novelTitle}》的第{chapterTitle}章。
            
            【大纲】
            {outline}
            
            【前文上下文】
            {context}
            
            【写作要求】
            - 字数：{targetWords}字左右
            - 风格：{style}
            - 保持与前文的连贯性
            - 合理分配段落，注意叙事节奏
            """;

    // ── AI润色 ──
    public static final String POLISH = """
            你是一位专业的文字编辑。请对以下段落进行润色，使其更加{styleType}。
            
            【原文】
            {originalText}
            
            【要求】
            - 保持原意不变
            - 仅优化表达方式
            - 不要添加新内容
            """;

    // ── 标题生成 ──
    public static final String GENERATE_TITLE = """
            你是一位专业小说作家，请为小说新章节生成标题。
            
            【当前章节】：第 {chapterIndex} 章
            【作品风格】：{genre}
            【标题基调】：{tone}
            
            【已有章节标题序列】：
            {existingTitles}
            
            【已有标题风格分析】：
            {styleAnalysis}
            
            【本章剧情方向】：
            {direction}
            
            【生成约束】：
            - 保持与已有标题统一的修辞风格和韵律节奏
            - 与上一章标题形成叙事递进或转折呼应
            - 避免重复使用已有标题中的核心意象词
            - 暗示本章核心事件或情感基调
            - 为下一章预留叙事空间
            
            【输出规范】：只输出标题本身（5-8个汉字），不要引号、标点或任何解释。
            """;

    // ── 章节内容生成（自然小说格式） ──
    public static final String GENERATE_CONTENT = """
            你是一位优秀的小说作家，正在续写小说《%s》的第 %d 章。

            【上一章结尾（必须从这一句之后开始写）】：
            %s

            【本章标题】：%s
            【本章方向】：%s
            【目标字数】：约 %d 字

            【强制格式要求】：
            1. 每个自然段结束后必须换行
            2. 段落之间必须有空行（即两个换行）
            3. 每段文字不超过 200 字
            4. 所有对话必须用「」括起来，并独立成行（前后各有空行）
            5. 场景转换时用「---」分隔（前后各空行）
            6. 使用感官描写（视觉/听觉/触觉/嗅觉）

            【叙事要求】：
            1. 从上一章结尾的「下一秒」开始写
            2. 不要复述或改写上一章的内容
            3. 保持人物状态、情感基调的连续性
            4. 默认读者已读过前文，直接从新内容开始

            【输出示例】：
            第一段内容。描写场景和氛围，每段不超过200字。

            第二段内容。进入新的动作或对话。

            「这是对话内容，独立成行。」

            第三段内容。继续推进情节。

            ---

            场景转换后的新段落。

            直接输出正文，不要添加任何额外说明：
            """;

    // ── AI扩写 ──
    public static final String EXPAND = """
            你是一位专业的小说作家。请对以下章节内容进行扩写，丰富细节描写和情节发展。
            
            【当前内容】
            {currentContent}
            
            【扩写方向】
            {direction}
            
            【写作风格】
            {style}
            
            【要求】
            - 保持原有文风和叙事节奏
            - 丰富细节描写（场景、动作、心理）
            - 合理扩展对话和内心独白
            - 扩写后内容不少于原内容的1.5倍
            - 保持情节连贯性
            """;

    // ── Agent: 编辑 ──
    public static final String EDITOR_AGENT = """
            你是一位资深小说编辑。请分析以下章节内容。
            
            【章节内容】
            {chapterContent}
            
            请从以下维度分析：
            1. 叙事节奏：是否有拖沓或过于急促之处？
            2. 逻辑一致性：是否存在前后矛盾？
            3. 伏笔管理：当前章节是否埋设/回收了伏笔？
            4. 改进建议：具体的修改建议
            """;

    // ── Agent: 人物 ──
    public static final String CHARACTER_AGENT = """
            你是一位人物塑造专家。请分析以下章节中的人物表现。
            
            【人物档案】
            {characterProfile}
            
            【章节内容】
            {chapterContent}
            
            请分析：
            1. 行为一致性：人物行为是否符合其设定？
            2. 对话风格：对话是否贴合人物性格？
            3. 弧光进度：人物的成长变化是否合理？
            """;

    // ── Agent: 风格 ──
    public static final String STYLE_AGENT = """
            你是一位文学风格分析专家。请分析以下文本的写作风格。
            
            【黄金样本（作者锁定章节）】
            {goldSamples}
            
            【待分析文本】
            {targetText}
            
            请从以下维度对比：
            1. 句式特征（句长、节奏）
            2. 词汇特征（高频词、成语率）
            3. 修辞密度（比喻、拟人）
            4. 叙事视角一致性
            5. 标点使用习惯
            """;

    // ── Agent: 读者 ──
    public static final String READER_AGENT = """
            你是一位{readerType}读者。请以该类型读者的视角，阅读以下章节并给出反馈。
            
            【章节内容】
            {chapterContent}
            
            请分享：
            1. 阅读感受：这一章给你什么感觉？
            2. 吸引点：哪些部分最吸引你？
            3. 疑惑点：哪些地方让你困惑？
            4. 期待：你希望接下来看到什么？
            """;

    // ════════════════════════════════════════
    // AI 先导式创作：6阶段设定生成模板
    // ════════════════════════════════════════

    // ── 阶段1：世界观生成 ──
    public static final String SETUP_WORLD = """
            你是一位世界构建专家。请根据以下信息，构建一个完整的幻想世界。
            
            【作品名称】：{title}
            【作品类型】：{genre}
            【核心灵感】：{inspiration}
            【风格基调】：{style}
            
            【输出要求】：请严格按以下JSON格式输出（不要输出其他内容）：
            {
              "era": "时代背景描述",
              "geography": "地理版图（含主要区域和地标）",
              "history": "历史年表（3个关键事件）",
              "powerSystem": "力量体系（等级、来源、规则）",
              "factions": [
                {"name": "势力名", "description": "描述", "goal": "目标"}
              ],
              "uniqueRules": "世界特殊规则或核心矛盾"
            }
            
            【约束】：所有设定必须与核心灵感「{inspiration}」紧密关联，保持「{style}」风格基调，每个字段200-400字。
            
            【重要】只输出纯JSON，不要任何解释、前缀或后缀。直接以 {{ 开头。
            """;

    // ── 阶段2：人物生成 ──
    public static final String SETUP_CHARACTERS = """
            你是一位角色塑造专家。请根据以下世界观，生成完整的人物群像。
            
            【作品名称】：{title}
            【作品类型】：{genre}
            【核心灵感】：{inspiration}
            【世界设定】：{world}
            【风格基调】：{style}
            
            【输出要求】：请严格按以下JSON格式输出5-8位人物：
            {
              "characters": [
                {
                  "name": "姓名",
                  "role": "protagonist|supporting|antagonist|minor",
                  "age": 数字,
                  "personality": "性格（3-5关键词）",
                  "background": "身世背景",
                  "motivation": "核心动机",
                  "arc": "成长弧光（起点→终点）",
                  "ability": "特殊能力",
                  "tags": ["标签1", "标签2"],
                  "relationships": [
                    {"targetName": "关联人物名", "type": "师徒|恋人|敌对|盟友|父子|其他"}
                  ]
                }
              ]
            }
            
            【约束】：至少1位主角、2位配角、1位反派；性格有冲突与互补；动机合理有深度；标签准确。
            
            【重要】只输出纯JSON，不要任何解释、前缀或后缀。直接以 {{ 开头。
            """;

    // ── 阶段3：大纲生成 ──
    public static final String SETUP_OUTLINE = """
            你是一位故事结构专家。请根据以下设定，生成完整的小说大纲。
            
            【作品名称】：{title}
            【作品类型】：{genre}
            【核心灵感】：{inspiration}
            【世界设定】：{world}
            【人物列表】：{characters}
            【风格基调】：{style}
            【目标章节数】：{targetChapters}
            
            【输出要求】：请按三幕式结构，输出完整章节序列。JSON格式：
            {
              "template": "three-act",
              "acts": [
                {
                  "name": "第一幕：建置",
                  "theme": "幕主题（一句话）",
                  "conflict": "幕核心冲突",
                  "chapters": [
                    {"index": 1, "title": "章节标题（5-8字）", "summary": "摘要（50字）", "keyEvent": "关键事件", "characters": ["出场人物名"]}
                  ]
                }
              ]
            }
            
            【约束】：章节标题5-8字与风格一致；章节间因果递进；关键人物合理出场；伏笔前1/3埋、后1/3收。
            
            【重要】只输出纯JSON，不要任何解释、前缀或后缀。直接以 {{ 开头。
            """;

    // ── 阶段4：情节引擎生成 ──
    public static final String SETUP_PLOT = """
            你是一位情节设计专家。请根据以下大纲，生成完整的情节线和伏笔系统。
            
            【作品名称】：{title}
            【作品类型】：{genre}
            【大纲】：{outline}
            【人物】：{characters}
            【风格基调】：{style}
            
            【输出要求】：JSON格式：
            {
              "mainThread": {
                "title": "主线标题",
                "description": "主线描述",
                "nodes": [
                  {"chapter": 章节号, "event": "事件描述", "type": "setup|conflict|climax|resolution"}
                ]
              },
              "subThreads": [
                {"title": "支线名", "description": "描述", "relatedChapters": [章节号数组], "involves": ["人物名"]}
              ],
              "foreshadowing": [
                {"title": "伏笔名", "buriedAt": 埋下章, "revealAt": 回收章, "hint": "线索提示", "importance": "core|minor"}
              ],
              "tensionCurve": [
                {"chapterRange": "1-10", "tension": 1-10, "label": "描述"}
              ]
            }
            
            【约束】：至少5个伏笔、2条支线；伏笔回收前至少呼应一次；张力曲线符合起承转合。
            
            【重要】只输出纯JSON，不要任何解释、前缀或后缀。直接以 {{ 开头。
            """;

    // ── 阶段5：灵感素材生成 ──
    public static final String SETUP_INSPIRATIONS = """
            你是一位创作灵感专家。请根据以下完整设定，生成丰富的灵感素材库。
            
            【作品名称】：{title}
            【作品类型】：{genre}
            【世界观】：{world}
            【人物】：{characters}
            【大纲】：{outline}
            【情节】：{plot}
            【风格基调】：{style}
            
            【输出要求】：请生成至少15条灵感素材，JSON格式：
            {
              "items": [
                {"category": "dialogue|scene|detail|reference", "content": "素材内容（30-100字）", "relatedTo": ["关联要素"], "usageHint": "使用建议章节或情境"}
              ]
            }
            
            【分类要求】：对白灵感≥4条、场景描写≥3条、人物细节≥4条、典故隐喻≥4条。每条标注可使用章节。
            
            【重要】只输出纯JSON，不要任何解释、前缀或后缀。直接以 {{ 开头。
            """;

    // ── 阶段6：设定感知的章节续写 ──
    public static final String SETUP_CONTINUE = """
            你是一位优秀的小说作家，擅长用流畅自然的语言续写小说章节。
            
            【作品信息】名称：{title}　类型：{genre}　风格：{style}
            
            【世界设定】{world}
            
            【人物列表】{characters}
            
            【大纲上下文】第{chapterIndex}章「{chapterTitle}」
            本章摘要：{chapterSummary}
            上一章摘要：{previousSummary}
            
            【情节状态】主线进度：{mainProgress}%　激活支线：{activeSubThreads}　待回收伏笔：{pendingForeshadowing}
            
            【写作要求】目标字数：{targetWords}字　推进内容：{direction}
            出场人物：{charactersInThisChapter}
            
            【格式要求】：
            1. 自然分段，段落间用空行分隔
            2. 所有对话必须用「」括起来，独立成行
            3. 场景转换时用「---」分隔
            4. 使用感官描写（视觉/听觉/触觉/嗅觉）
            5. 每段不超过200字，长短交替，形成节奏感
            6. 【重要】不要添加任何结构标签（如"开篇场景"、"发展"、"高潮"、"结尾"等），直接输出正文
            7. 【重要】不要输出章节标题，直接开始写正文内容
            
            直接输出正文：
            """;

    // ── 伏笔追加补写 ──
    public static final String APPEND_FORESHADOWING = """
            你是一位专业的小说作家。请在尊重原文风格的前提下，完成伏笔融入任务。

            【伏笔信息】
            标题：{foreshadowingTitle}
            描述：{foreshadowingDescription}

            【章节原文】
            {originalContent}

            【任务要求】
            1. 不修改已有内容，仅追加或插入相关段落
            2. 插入内容应与上下文自然衔接，文风一致
            3. 伏笔应通过角色对话、心理活动或环境细节自然体现，不刻意突兀
            4. 新增部分控制在 100~300 字之间
            5. 只输出需要追加的段落内容，不要重写全文
            6. 保持原有叙事节奏，不要破坏已有情节的连贯性
            7. 直接输出补写内容，不要添加任何说明或标注

            【输出格式】
            仅输出追加的段落内容：
            """;
}