import java.util.*;

class Player {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int baseX = in.nextInt(); // The corner of the map representing your base
        int baseY = in.nextInt();
        int heroesPerPlayer = in.nextInt(); // Always 3

        // game loop

        GamePlayer me;
        GamePlayer ennemy;

        Pos basePos = new Pos(baseX, baseY);

        Pos z1 = HeroModule.zone1(Pos.SCREEN_LEFT_UP);
        Pos z2 = HeroModule.zone2(z1);
        Pos z3 = HeroModule.zone3(z2);

        z1 = z1.reverseWhenOnRight(basePos);
        z2 = z2.reverseWhenOnRight(basePos);
        z3 = z3.reverseWhenOnRight(basePos);

        Pos wind = new Pos(1, 1);
        if (!basePos.isOnLeftSide()) {
            wind = new Pos(-1, -1);
        }

        System.err.println("base " + basePos);

        System.err.println("Z1 " + z1);
        System.err.println("Z2 " + z2);
        System.err.println("Z3 " + z3);

        System.err.println("cZ1 " + HeroModule.center(z1));
        System.err.println("cZ2 " + HeroModule.center(z2));
        System.err.println("cZ3 " + HeroModule.center(z3));


        while (true) {

            List<Hero> myHeroes = new ArrayList<>();
            List<Hero> hisHeroes = new ArrayList<>();
            List<Monster> monsters = new ArrayList<>();

            for (int i = 0; i < 2; i++) {
                int health = in.nextInt(); // Each player's base health
                int mana = in.nextInt(); // Ignore in the first league; Spend ten mana to cast a spell
                GamePlayer current = new GamePlayer(health, mana);
                if (i == 0) {
                    me = current;
                } else {
                    ennemy = current;
                }
            }
            int entityCount = in.nextInt(); // Amount of heros and monsters you can see
            for (int i = 0; i < entityCount; i++) {
                int id = in.nextInt(); // Unique identifier
                int type = in.nextInt(); // 0=monster, 1=your hero, 2=opponent hero
                int x = in.nextInt(); // Position of this entity
                int y = in.nextInt();
                int shieldLife = in.nextInt(); // Ignore for this league; Count down until shield spell fades
                int isControlled = in.nextInt(); // Ignore for this league; Equals 1 when this entity is under a control spell
                int health = in.nextInt(); // Remaining health of this monster
                int vx = in.nextInt(); // Trajectory of this monster
                int vy = in.nextInt();
                int nearBase = in.nextInt(); // 0=monster with no target yet, 1=monster targeting a base
                int threatFor = in.nextInt(); // Given this monster's trajectory, is it a threat to 1=your base, 2=your opponent's base, 0=neither

                ThreatFor threatForEnum;
                boolean nearBaseBool = nearBase == 1;
                if (nearBaseBool) {
                    if (threatFor == 1) {
                        threatForEnum = ThreatFor.MINE;
                    } else {
                        threatForEnum = ThreatFor.HIS;
                    }
                } else {
                    if (threatFor == 0) {
                        threatForEnum = ThreatFor.NO_ONE;
                    } else if (threatFor ==  1) {
                        threatForEnum = ThreatFor.NEXT_MINE;
                    } else {
                        threatForEnum = ThreatFor.NEXT_HIS;
                    }
                }

                if (type == 0) {
                    monsters.add(new Monster(id, health, new Pos(x, y), new Vitesse(vx, vy), nearBaseBool, threatForEnum));
                } else {
                    Hero hero = new Hero(id, new Pos(x, y));

                    if (type == 1) {
                        myHeroes.add(hero);
                    } else {
                        hisHeroes.add(hero);
                    }
                }
            }

            myHeroes.sort(Comparator.comparing(h -> h.id));

            //System.err.println("My Heroes ID " + myHeroes);
            Map<Integer, Monster> monsterPerHero = new HashMap<>();
            Map<Integer, Pos> zonePerHero = new HashMap<>();

            Monster mz1 = MonsterModule.chooseTarget(monsters, z1, basePos);
            monsterPerHero.put(myHeroes.get(0).id, mz1);
            zonePerHero.put(myHeroes.get(0).id, z1);

            Monster mz2 = MonsterModule.chooseTarget(monsters, z2, basePos);
            monsterPerHero.put(myHeroes.get(1).id, mz2);
            zonePerHero.put(myHeroes.get(1).id, z2);

            Monster mz3 = MonsterModule.chooseTarget(monsters, z3, basePos);
            monsterPerHero.put(myHeroes.get(2).id, mz3);
            zonePerHero.put(myHeroes.get(2).id, z3);

            System.err.println("Target Monsters " + monsterPerHero.values());
            for (int i = 0; i < heroesPerPlayer; i++) {

                // Write an action using System.out.println()
                // To debug: System.err.println("Debug messages...");
                Hero currentHero = myHeroes.get(i);
                Monster currentTarget = monsterPerHero.get(currentHero.id);

                // In the first league: MOVE <x> <y> | WAIT; In later leagues: | SPELL <spellParams>;
                String action = "WAIT wait for it";
                if (currentTarget != null) {
                    System.err.println("CurrentTarget " + currentTarget.id);
                    Pos nextPos = currentTarget.nextPos();
                    boolean useWindSpell = HeroModule.useWindSpell(basePos, currentHero.pos, currentTarget);
                    if (useWindSpell) {
                        Pos windVec = HeroModule.wind(currentHero.pos, wind);
                        action = String.format("SPELL WIND %d %d WindSpell", windVec.x, windVec.y);
                    } else {
                        action = String.format("MOVE %d %d Chase", nextPos.x, nextPos.y);
                    }
                } else {
                    Pos zone = zonePerHero.get(currentHero.id);
                    Pos center = HeroModule.center(zone);
                    //System.err.println("Center " + center);
                    //System.err.println("Hero " + currentHero.pos);
                    if (!currentHero.pos.equals(center)) {
                        action = String.format("MOVE %d %d Go to center", center.x, center.y);
                    }
                }
                System.out.println(action);
            }
        }
    }

    static class GamePlayer {
        int health;
        int mana;

        public GamePlayer(int health, int mana) {
            this.health = health;
            this.mana = mana;
        }
    }

    static class HeroModule {

        public static boolean isMonsterThreatenBase(Monster monster) {
            return ThreatFor.MINE.equals(monster.threatFor) || ThreatFor.NEXT_MINE.equals(monster.threatFor);
        }

        public static boolean isMonsterInZone(Pos zone, Pos monsterPos) {
            //System.err.println("isMonsterZone " + zone + "/" + zone.rightCorner() + " | " + monsterPos);
            boolean r = ((monsterPos.x >= zone.x && monsterPos.x < maxX(zone))
                    && (monsterPos.y >= zone.y && monsterPos.y < maxY(zone)));
            System.err.println("isMonsterZone r=" + r);
            return r;
        }

        public static Pos zone1(Pos basePos) {
            int baseX1 = minX(basePos);
            int baseY1 = Math.max(0, basePos.y - Pos.ZONE_HEIGHT);
            return new Pos(baseX1, baseY1);
        }

        public static Pos zone2(Pos z1) {
            return new Pos(z1.x, Math.min(9000, z1.y + Pos.ZONE_HEIGHT));
        }

        public static Pos zone3(Pos z2) {
            return new Pos(z2.x, Math.min(9000, z2.y + Pos.ZONE_HEIGHT));
        }

        public static Pos center(Pos pos) {
            return new Pos(pos.x + (Pos.ZONE_WIDTH / 2), pos.y + (Pos.ZONE_HEIGHT / 2));
        }

        static int minX(Pos basePos) {
            return basePos.x + Pos.GAP;
        }
        static int maxX(Pos pos) { return pos.x + Pos.ZONE_WIDTH; }
        static int maxY(Pos pos) { return pos.y + Pos.ZONE_HEIGHT; }

        static Pos wind(Pos hero, Pos windVec) {
            int x = hero.x + (windVec.x * 100);
            int y = hero.y + (windVec.y * 100);
            return new Pos(x, y);
        }

        static boolean useWindSpell(Pos base, Pos hero, Monster monster) {
            if (MonsterModule.threatLevelByDistance(base, monster.pos) >= 1.0d) {
                return true;
            }
            return false;
        }
    }

    static class MonsterModule {

        public static Monster chooseTarget(List<Monster> monsters, Pos zone, Pos base) {
            Monster target = monsters.stream()
                    .filter(HeroModule::isMonsterThreatenBase)
                    .filter(m -> HeroModule.isMonsterInZone(zone, m.pos))
                    .map(m -> {
                        //System.err.println("Threat detected");
                        return new ThreatMonster(MonsterModule.threatLevel(base, m), m);
                    })
                    .max(Comparator.comparing(tm -> tm.threadLevel))
                    .map(tm -> {
                        //System.err.println("Extract monster object");
                        return tm.monster;
                    })
                    .map(tm -> {
                        //System.err.println("Target : " + zone + " / " + tm.id);
                        return tm;
                    })
                    .orElse(null);
            return target;
        }

        static double threatLevel(Pos basePos, Monster m) {
            double dist = threatLevelByDistance(basePos, m.pos);
            double proximity = m.threatFor.equals(ThreatFor.MINE) ? 50 : 0;
            return dist + proximity;
        }

        static double threatLevelByDistance(Pos basePos, Pos monsterPos) {
            double dist = Math.hypot(Math.abs(monsterPos.x - basePos.x), Math.abs(monsterPos.y - basePos.y));
            return (1000 / (dist + 1));
        }
    }

    static class Monster {
        int id;
        int health;
        Pos pos;
        Vitesse vitesse;
        boolean nearBase;
        ThreatFor threatFor;

        public Monster(int id, int health, Pos pos, Vitesse vitesse, boolean nearBase, ThreatFor threatFor) {
            this.id = id;
            this.health = health;
            this.pos = pos;
            this.vitesse = vitesse;
            this.nearBase = nearBase;
            this.threatFor = threatFor;
        }

        Pos nextPos() {
            return pos;
        }


    }

    static class ThreatMonster {
        Double threadLevel;
        Monster monster;

        public ThreatMonster(double threadLevel, Monster monster) {
            this.threadLevel = threadLevel;
            this.monster = monster;
        }
    }

    static class Hero {
        int id;
        Pos pos;

        public Hero(int id, Pos pos) {
            this.id = id;
            this.pos = pos;
        }

        @Override
        public String toString() {
            return "Hero{" +
                    "id=" + id +
                    '}';
        }
    }


    static class Pos {

        public static int GAP = 10;
        public static int ZONE_WIDTH = 6000;
        public static int ZONE_HEIGHT = 2500;

        public static Pos SCREEN_LEFT_UP = new Pos(0, 0);
        public static Pos SCREEN_RIGHT_BOTTOM = new Pos(17630, 9000);

        int x;
        int y;

        public Pos(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Pos rightCorner() {
            return new Pos(x + ZONE_WIDTH, y + ZONE_HEIGHT);
        }

        public boolean isOnLeftSide() {
            return x < (SCREEN_RIGHT_BOTTOM.x / 2);
        }

        int reverseX() {
            return SCREEN_RIGHT_BOTTOM.x - ZONE_WIDTH - GAP;
        }

        int reverseY() {
            return y + ZONE_HEIGHT;
        }

        public Pos reverseWhenOnRight(Pos base) {
            if (base.isOnLeftSide()) {
                return this;
            }
            return new Pos(this.reverseX(), this.reverseY());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pos pos = (Pos) o;
            return x == pos.x && y == pos.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "Pos{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    static class Vitesse {
        int vx;
        int vy;

        public Vitesse(int vx, int vy) {
            this.vx = vx;
            this.vy = vy;
        }
    }

    enum ThreatFor {
        NO_ONE,
        NEXT_MINE,
        NEXT_HIS,
        MINE,
        HIS;
    }
}