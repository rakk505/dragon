package com.modernity.dragon.client.model;

import com.modernity.dragon.client.renderer.state.DragonBossRenderState;
import com.modernity.dragon.entity.DragonBossEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A fully hierarchical, cuboid-native Chinese dragon rig.
 *
 * <p>Minecraft 26.2 folded the old {@code HierarchicalModel} contract into
 * {@link EntityModel}; {@link EntityModel#root()} exposes the same hierarchy.
 * The sixteen {@code axial_XX} bones form one uninterrupted kinematic chain so
 * procedural waves and the coil attack remain continuous from neck to tail.</p>
 */
public final class DragonBossModel extends EntityModel<DragonBossRenderState> {
    public static final int AXIAL_SEGMENT_COUNT = 16;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    // A deliberately uneven four-beat sequence keeps the limbs from reading as mirrored pairs.
    private static final float[] LEG_PHASE_OFFSETS = {0.0F, 2.17F, 4.49F, 5.73F};
    private static final float[] LEG_RATE_OFFSETS = {0.0F, 0.008F, -0.006F, 0.004F};

    private final ModelPart dragonRoot;
    private final ModelPart[] axial = new ModelPart[AXIAL_SEGMENT_COUNT];
    private final ModelPart[] tail = new ModelPart[4];
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart throat;
    private final ModelPart maneCrown;
    private final ModelPart beard;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;
    private final ModelPart[][] whiskers = new ModelPart[2][3];
    private final ModelPart[] upperLegs = new ModelPart[4];
    private final ModelPart[] lowerLegs = new ModelPart[4];
    private final ModelPart[] paws = new ModelPart[4];

    public DragonBossModel(ModelPart root) {
        super(root);
        this.dragonRoot = root.getChild("dragon");

        ModelPart cursor = this.dragonRoot;
        for (int i = 0; i < AXIAL_SEGMENT_COUNT; i++) {
            cursor = cursor.getChild(axialName(i));
            this.axial[i] = cursor;
        }

        this.head = this.axial[0].getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.throat = this.head.getChild("throat");
        this.maneCrown = this.head.getChild("mane_crown");
        this.beard = this.head.getChild("beard");
        this.leftHorn = this.head.getChild("horn_left_0");
        this.rightHorn = this.head.getChild("horn_right_0");

        bindWhisker(this.head, 0, "whisker_left");
        bindWhisker(this.head, 1, "whisker_right");
        bindLeg(this.axial[2], 0, "front_left");
        bindLeg(this.axial[2], 1, "front_right");
        bindLeg(this.axial[10], 2, "hind_left");
        bindLeg(this.axial[10], 3, "hind_right");

        cursor = this.axial[AXIAL_SEGMENT_COUNT - 1];
        for (int i = 0; i < this.tail.length; i++) {
            cursor = cursor.getChild("tail_" + i);
            this.tail[i] = cursor;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Z is offset so the visual midpoint, rather than the head, rotates at the entity origin.
        PartDefinition dragon = root.addOrReplaceChild("dragon", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -82.0F));

        int[] widths = {18, 19, 20, 20, 19, 19, 18, 18, 17, 17, 16, 16, 15, 14, 13, 12};
        int[] heights = {17, 18, 19, 19, 18, 18, 17, 17, 16, 16, 15, 15, 14, 13, 12, 11};
        PartDefinition[] axial = new PartDefinition[AXIAL_SEGMENT_COUNT];
        PartDefinition cursor = dragon;
        for (int i = 0; i < AXIAL_SEGMENT_COUNT; i++) {
            PartPose pose = i == 0 ? PartPose.ZERO : PartPose.offset(0.0F, 0.0F, 10.0F);
            cursor = cursor.addOrReplaceChild(axialName(i), axialCubes(widths[i], heights[i], i), pose);
            axial[i] = cursor;
        }

        PartDefinition head = addHead(axial[0]);
        addHorn(head, "horn_left", 1);
        addHorn(head, "horn_right", -1);
        addWhisker(head, "whisker_left", 1);
        addWhisker(head, "whisker_right", -1);

        addLeg(axial[2], "front_left", 1, false);
        addLeg(axial[2], "front_right", -1, false);
        addLeg(axial[10], "hind_left", 1, true);
        addLeg(axial[10], "hind_right", -1, true);
        addTail(axial[AXIAL_SEGMENT_COUNT - 1]);

        return LayerDefinition.create(mesh, 256, 256);
    }

    private static CubeListBuilder axialCubes(int width, int height, int index) {
        int crestHeight = index < 11 ? 7 - index / 4 : 4;
        return CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-width / 2.0F, -height / 2.0F, -1.0F, width, height, 12)
                .texOffs(0, 52)
                .addBox(-width / 2.0F + 2.0F, height / 2.0F - 1.5F, 0.0F, width - 4, 2, 10)
                .texOffs(96, 0)
                .addBox(-2.0F, -height / 2.0F - crestHeight + 1.0F, 0.5F, 4, crestHeight, 4)
                .texOffs(96, 0)
                .addBox(-1.5F, -height / 2.0F - crestHeight - 1.0F, 5.0F, 3, crestHeight + 1, 4);
    }

    private static PartDefinition addHead(PartDefinition neck) {
        PartDefinition head = neck.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 112)
                        .addBox(-10.0F, -10.0F, -22.0F, 20, 18, 22)
                        .texOffs(0, 112)
                        .addBox(-8.5F, -3.0F, -36.0F, 17, 9, 18)
                        .texOffs(0, 112)
                        .addBox(-12.0F, -2.0F, -20.0F, 4, 10, 11)
                        .texOffs(0, 112)
                        .addBox(8.0F, -2.0F, -20.0F, 4, 10, 11)
                        .texOffs(96, 112)
                        .addBox(-9.0F, -1.5F, -38.0F, 18, 5, 5)
                        .texOffs(160, 112)
                        .addBox(-10.75F, -7.0F, -19.0F, 2, 4, 7)
                        .texOffs(160, 112)
                        .addBox(8.75F, -7.0F, -19.0F, 2, 4, 7)
                        .texOffs(96, 112)
                        .addBox(-5.5F, 5.0F, -35.5F, 2, 3, 2)
                        .texOffs(96, 112)
                        .addBox(3.5F, 5.0F, -35.5F, 2, 3, 2),
                PartPose.offset(0.0F, -1.0F, 0.0F)
        );

        head.addOrReplaceChild(
                "jaw",
                CubeListBuilder.create()
                        .texOffs(96, 112)
                        .addBox(-8.0F, -1.0F, -20.0F, 16, 4, 20)
                        .texOffs(160, 52)
                        .addBox(-6.5F, -3.0F, -18.0F, 2, 4, 2)
                        .texOffs(160, 52)
                        .addBox(-2.5F, -3.0F, -19.0F, 2, 4, 2)
                        .texOffs(160, 52)
                        .addBox(0.5F, -3.0F, -19.0F, 2, 4, 2)
                        .texOffs(160, 52)
                        .addBox(4.5F, -3.0F, -18.0F, 2, 4, 2),
                PartPose.offset(0.0F, 6.0F, -15.0F)
        );

        head.addOrReplaceChild(
                "throat",
                CubeListBuilder.create()
                        .texOffs(0, 52)
                        .addBox(-6.0F, 0.0F, -10.0F, 12, 6, 15)
                        .texOffs(96, 0)
                        .addBox(-4.0F, 4.0F, -7.0F, 8, 5, 8),
                PartPose.offset(0.0F, 6.0F, -9.0F)
        );

        head.addOrReplaceChild(
                "mane_crown",
                CubeListBuilder.create()
                        .texOffs(96, 0)
                        .addBox(-4.0F, -8.0F, -19.0F, 8, 8, 7)
                        .texOffs(96, 0)
                        .addBox(-5.0F, -10.0F, -13.0F, 10, 10, 7)
                        .texOffs(96, 0)
                        .addBox(-4.0F, -9.0F, -7.0F, 8, 9, 8)
                        .texOffs(96, 0)
                        .addBox(-13.0F, -2.0F, -16.0F, 5, 12, 8)
                        .texOffs(96, 0)
                        .addBox(8.0F, -2.0F, -16.0F, 5, 12, 8),
                PartPose.offset(0.0F, -8.0F, 0.0F)
        );

        head.addOrReplaceChild(
                "beard",
                CubeListBuilder.create()
                        .texOffs(96, 0)
                        .addBox(-5.0F, 0.0F, -7.0F, 10, 8, 8)
                        .texOffs(96, 0)
                        .addBox(-3.5F, 6.0F, -5.0F, 7, 7, 6)
                        .texOffs(96, 0)
                        .addBox(-2.0F, 11.0F, -3.0F, 4, 7, 4),
                PartPose.offset(0.0F, 7.0F, -12.0F)
        );

        head.addOrReplaceChild(
                "ear_left",
                CubeListBuilder.create().texOffs(96, 0).addBox(0.0F, -4.0F, -2.0F, 7, 8, 4),
                PartPose.offsetAndRotation(8.0F, -6.0F, -7.0F, -0.18F, -0.30F, -0.35F)
        );
        head.addOrReplaceChild(
                "ear_right",
                CubeListBuilder.create().texOffs(96, 0).mirror().addBox(-7.0F, -4.0F, -2.0F, 7, 8, 4),
                PartPose.offsetAndRotation(-8.0F, -6.0F, -7.0F, -0.18F, 0.30F, 0.35F)
        );
        return head;
    }

    private static void addHorn(PartDefinition head, String prefix, int side) {
        PartDefinition horn0 = head.addOrReplaceChild(
                prefix + "_0",
                CubeListBuilder.create().texOffs(160, 52).addBox(-2.0F, -11.0F, -2.0F, 4, 12, 4),
                PartPose.offsetAndRotation(side * 6.5F, -8.0F, -8.0F, -0.28F, side * 0.16F, side * 0.42F)
        );
        PartDefinition horn1 = horn0.addOrReplaceChild(
                prefix + "_1",
                CubeListBuilder.create().texOffs(160, 52).addBox(-1.5F, -10.0F, -1.5F, 3, 11, 3),
                PartPose.offsetAndRotation(0.0F, -9.5F, 0.0F, 0.34F, side * 0.20F, -side * 0.30F)
        );
        horn1.addOrReplaceChild(
                prefix + "_2",
                CubeListBuilder.create().texOffs(160, 52).addBox(-1.0F, -10.0F, -1.0F, 2, 11, 2),
                PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, 0.20F, side * 0.18F, side * 0.24F)
        );
        horn1.addOrReplaceChild(
                prefix + "_branch",
                CubeListBuilder.create().texOffs(160, 52).addBox(-1.0F, -8.0F, -1.0F, 2, 9, 2),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, -0.55F, side * 0.40F, side * 0.82F)
        );
    }

    private static void addWhisker(PartDefinition head, String prefix, int side) {
        PartDefinition whisker0 = head.addOrReplaceChild(
                prefix + "_0",
                CubeListBuilder.create().texOffs(192, 112).addBox(-0.5F, -0.5F, -12.0F, 1, 1, 13),
                PartPose.offsetAndRotation(side * 7.0F, 2.0F, -29.0F, 0.12F, side * 0.72F, -side * 0.10F)
        );
        PartDefinition whisker1 = whisker0.addOrReplaceChild(
                prefix + "_1",
                CubeListBuilder.create().texOffs(192, 112).addBox(-0.5F, -0.5F, -12.0F, 1, 1, 13),
                PartPose.offsetAndRotation(0.0F, 0.0F, -11.0F, -0.20F, side * 0.36F, side * 0.12F)
        );
        whisker1.addOrReplaceChild(
                prefix + "_2",
                CubeListBuilder.create().texOffs(192, 112).addBox(-0.5F, -0.5F, -10.0F, 1, 1, 11),
                PartPose.offsetAndRotation(0.0F, 0.0F, -11.0F, -0.24F, side * 0.32F, side * 0.14F)
        );
    }

    private static void addLeg(PartDefinition body, String prefix, int side, boolean hind) {
        PartDefinition upper = body.addOrReplaceChild(
                prefix + "_upper",
                CubeListBuilder.create()
                        .texOffs(96, 52)
                        .addBox(-3.5F, -1.0F, -3.5F, 7, 12, 7)
                        .texOffs(96, 0)
                        .addBox(-4.5F, -2.0F, -4.5F, 9, 5, 9),
                PartPose.offsetAndRotation(side * (hind ? 6.5F : 8.0F), 2.0F, hind ? 6.0F : 1.0F, hind ? 0.18F : -0.08F, 0.0F, side * 0.24F)
        );
        PartDefinition lower = upper.addOrReplaceChild(
                prefix + "_lower",
                CubeListBuilder.create()
                        .texOffs(96, 52)
                        .addBox(-2.5F, -1.0F, -2.5F, 5, 11, 5)
                        .texOffs(0, 52)
                        .addBox(-3.0F, 7.0F, -3.0F, 6, 5, 6),
                PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, hind ? -0.28F : 0.18F, 0.0F, -side * 0.12F)
        );
        lower.addOrReplaceChild(
                prefix + "_paw",
                CubeListBuilder.create()
                        .texOffs(0, 52)
                        .addBox(-4.0F, -1.0F, -7.0F, 8, 4, 9)
                        .texOffs(160, 52)
                        .addBox(-3.5F, 1.0F, -11.0F, 1, 2, 5)
                        .texOffs(160, 52)
                        .addBox(-0.5F, 1.0F, -12.0F, 1, 2, 6)
                        .texOffs(160, 52)
                        .addBox(2.5F, 1.0F, -11.0F, 1, 2, 5),
                PartPose.offsetAndRotation(0.0F, 10.0F, hind ? 1.0F : -1.0F, 0.16F, 0.0F, 0.0F)
        );
    }

    private static void addTail(PartDefinition lastAxial) {
        PartDefinition cursor = lastAxial;
        int[] sizes = {10, 8, 6, 4};
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            CubeListBuilder cubes = CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-size / 2.0F, -size / 2.0F, -1.0F, size, size, 12)
                    .texOffs(0, 52)
                    .addBox(-Math.max(1.0F, size / 2.0F - 2.0F), size / 2.0F - 1.0F, 0.0F, Math.max(2, size - 4), 2, 10)
                    .texOffs(96, 0)
                    .addBox(-1.0F, -size / 2.0F - 4.0F, 3.0F, 2, 5, 4);
            cursor = cursor.addOrReplaceChild("tail_" + i, cubes, PartPose.offset(0.0F, 0.0F, 10.0F));
        }
        cursor.addOrReplaceChild(
                "tail_fan",
                CubeListBuilder.create()
                        .texOffs(96, 0)
                        .addBox(-8.0F, -1.5F, -1.0F, 16, 3, 9)
                        .texOffs(96, 0)
                        .addBox(-5.0F, -4.0F, 2.0F, 10, 8, 8)
                        .texOffs(96, 0)
                        .addBox(-2.0F, -6.0F, 6.0F, 4, 12, 7),
                PartPose.offset(0.0F, 0.0F, 10.0F)
        );
    }

    private void bindWhisker(ModelPart head, int index, String prefix) {
        ModelPart cursor = head;
        for (int i = 0; i < this.whiskers[index].length; i++) {
            cursor = cursor.getChild(prefix + "_" + i);
            this.whiskers[index][i] = cursor;
        }
    }

    private void bindLeg(ModelPart body, int index, String prefix) {
        this.upperLegs[index] = body.getChild(prefix + "_upper");
        this.lowerLegs[index] = this.upperLegs[index].getChild(prefix + "_lower");
        this.paws[index] = this.lowerLegs[index].getChild(prefix + "_paw");
    }

    @Override
    public void setupAnim(DragonBossRenderState state) {
        super.setupAnim(state);
        float time = state.ageInTicks;
        float attackTicks = state.attackTicks;
        int attack = state.attackState;

        animateAxialChain(state);
        animateHead(state);
        animateWhiskers(state);
        animateLegs(state);

        float bobScale = attack == DragonBossEntity.ATTACK_RUSH && attackTicks >= 20.0F ? 0.15F : 1.0F;
        this.dragonRoot.y += Mth.sin(time * 0.12F) * 1.25F * bobScale;
        this.dragonRoot.x += Mth.sin(time * 0.075F) * 0.65F * bobScale;

        if (attack == DragonBossEntity.ATTACK_COIL && attackTicks >= 60.0F && attackTicks < 70.0F) {
            float charge = ease((attackTicks - 60.0F) / 10.0F);
            this.dragonRoot.x += Mth.sin(attackTicks * 3.4F) * 0.45F * charge;
            this.dragonRoot.y += Mth.cos(attackTicks * 2.9F) * 0.30F * charge;
            this.leftHorn.zRot += Mth.sin(attackTicks * 3.0F) * 0.035F * charge;
            this.rightHorn.zRot -= Mth.sin(attackTicks * 3.0F) * 0.035F * charge;
        }
    }

    /**
     * Treats the body as a continuous chain of angular joints. The primary
     * yaw and pitch waves use different wavelengths, so the silhouette bends
     * visibly both sideways and vertically instead of rotating as one rigid
     * tube. A crest begins at the neck and reaches each successive joint a few
     * frames later; the increasing envelope gives the loose tail the greatest
     * travel while keeping the head readable and aimable.
     */
    private void animateAxialChain(DragonBossRenderState state) {
        final int chainLength = this.axial.length + this.tail.length;
        float movement = Mth.clamp(state.flightSpeed / 1.25F, 0.0F, 1.0F);
        float baseTempo = 0.175F;
        float baseYawAmplitude = 0.105F + movement * 0.040F;
        float basePitchAmplitude = 0.073F + movement * 0.030F;
        float tempo = baseTempo;
        float yawAmplitude = baseYawAmplitude;
        float pitchAmplitude = basePitchAmplitude;
        float yawSpacing = 0.50F;
        float pitchSpacing = 0.405F;
        float attackTicks = state.attackTicks;
        int attack = state.attackState;
        float attackBlend = attackMotionWeight(attack, attackTicks);
        float coil = 0.0F;

        if (attack == DragonBossEntity.ATTACK_RUSH) {
            if (attackTicks < 20.0F) {
                // Gather into a strong S curve before springing forward.
                float windup = ease(attackTicks / 20.0F);
                tempo += windup * 0.075F;
                yawAmplitude *= 1.0F + windup * 0.72F;
                pitchAmplitude *= 1.0F + windup * 0.42F;
                yawSpacing += windup * 0.055F;
            } else if (attackTicks < 50.0F) {
                // The head streamlines during the dash; motion remains in the
                // rear body and arrives there after the front has straightened.
                tempo = 0.29F;
                yawAmplitude = 0.095F;
                pitchAmplitude = 0.060F;
            } else {
                float recovery = ease((attackTicks - 50.0F) / 10.0F);
                tempo = 0.24F - recovery * 0.045F;
                yawAmplitude *= 0.48F + recovery * 0.52F;
                pitchAmplitude *= 0.55F + recovery * 0.45F;
            }
        } else if (attack == DragonBossEntity.ATTACK_COIL) {
            float tightened = ease(attackTicks / 60.0F);
            float released = attackTicks < 70.0F ? 0.0F : ease((attackTicks - 70.0F) / 24.0F);
            coil = tightened * (1.0F - released);
            // Do not freeze the coiled dragon: a compact travelling ripple
            // continues through the ring while it stores energy.
            tempo = 0.20F + coil * 0.055F;
            yawAmplitude *= 1.0F - coil * 0.58F;
            pitchAmplitude *= 1.0F - coil * 0.32F;
            yawSpacing = 0.53F;
            pitchSpacing = 0.46F;
        } else if (attack == DragonBossEntity.ATTACK_FIREBALL) {
            float aim = ease(attackTicks / 16.0F);
            tempo = 0.135F;
            yawAmplitude *= 1.0F - aim * 0.28F;
            pitchAmplitude *= 1.0F - aim * 0.32F;
        } else if (attack == DragonBossEntity.ATTACK_SONIC_BOOM) {
            float aim = ease(attackTicks / 30.0F);
            tempo = 0.145F - aim * 0.025F;
            yawAmplitude *= 1.0F - aim * 0.46F;
            pitchAmplitude *= 1.0F - aim * 0.48F;
        } else if (attack == DragonBossEntity.ATTACK_LIGHTNING) {
            float charge = ease(attackTicks / 24.0F);
            tempo = 0.155F + charge * 0.035F;
            yawAmplitude *= 1.0F - charge * 0.34F;
            pitchAmplitude *= 1.0F - charge * 0.38F;
        }

        // Anchor special attack rhythms to the phase at which the attack
        // began, then cross-fade them back to the perpetual flight rhythm.
        // This avoids visible phase pops when a state changes or expires.
        float attackStart = state.ageInTicks - attackTicks;
        float baseYawClock = state.ageInTicks * baseTempo;
        float basePitchClock = state.ageInTicks * baseTempo * 0.82F + 1.18F;
        float attackYawClock = attackStart * baseTempo + attackTicks * tempo;
        float attackPitchClock = attackStart * baseTempo * 0.82F + attackTicks * tempo * 0.82F + 1.18F;
        for (int i = 0; i < chainLength; i++) {
            ModelPart segment = i < this.axial.length ? this.axial[i] : this.tail[i - this.axial.length];
            float chainProgress = i / (float) (chainLength - 1);
            float envelope = 0.72F + chainProgress * 0.70F;
            float segmentScale = attackSegmentScale(attack, attackTicks, chainProgress);
            float baseYawPhase = baseYawClock - i * 0.50F;
            float basePitchPhase = basePitchClock - i * 0.405F;
            float attackYawPhase = attackYawClock - i * yawSpacing;
            float attackPitchPhase = attackPitchClock - i * pitchSpacing;

            // The second, weaker harmonics keep the curves from looking like
            // a mechanically perfect sine wave without breaking continuity.
            float baseYawWave = Mth.sin(baseYawPhase) + Mth.sin(baseYawPhase * 2.0F + 0.65F) * 0.16F;
            float attackYawWave = Mth.sin(attackYawPhase) + Mth.sin(attackYawPhase * 2.0F + 0.65F) * 0.16F;
            float basePitchWave = Mth.sin(basePitchPhase) + Mth.sin(basePitchPhase * 1.63F - 0.40F) * 0.13F;
            float attackPitchWave = Mth.sin(attackPitchPhase) + Mth.sin(attackPitchPhase * 1.63F - 0.40F) * 0.13F;
            float yawWave = baseYawWave + (attackYawWave - baseYawWave) * attackBlend;
            float pitchWave = basePitchWave + (attackPitchWave - basePitchWave) * attackBlend;
            float activeYawAmplitude = baseYawAmplitude + (yawAmplitude - baseYawAmplitude) * attackBlend;
            float activePitchAmplitude = basePitchAmplitude + (pitchAmplitude - basePitchAmplitude) * attackBlend;
            float rollWave = Mth.sin(baseYawPhase + 1.57F)
                    + (Mth.sin(attackYawPhase + 1.57F) - Mth.sin(baseYawPhase + 1.57F)) * attackBlend;
            segment.yRot += yawWave * activeYawAmplitude * envelope * segmentScale;
            segment.xRot += pitchWave * activePitchAmplitude * envelope * segmentScale;
            segment.zRot += rollWave * activeYawAmplitude * 0.22F * envelope * segmentScale;

            // A near-constant local yaw produces a full coil. The slower
            // vertical wave prevents the attack from becoming a flat ring.
            segment.yRot += coil * (0.326F + Mth.sin(i * 0.71F - attackTicks * 0.045F) * 0.020F);
            segment.xRot += coil * Mth.sin(i * 0.47F - attackTicks * 0.095F) * 0.060F;

            if (attack == DragonBossEntity.ATTACK_COIL && attackTicks >= 70.0F) {
                // The release is a localized whip impulse that physically
                // travels from the neck to the tail instead of posing every
                // joint at once.
                float releaseAge = attackTicks - 70.0F;
                float crest = releaseAge * 0.78F;
                float impulse = ease(1.0F - Math.abs(i - crest) / 4.5F)
                        * (1.0F - ease(releaseAge / 29.0F));
                segment.yRot += Mth.sin(releaseAge * 0.82F - i * 0.88F) * impulse * 0.31F;
                segment.xRot -= Mth.cos(releaseAge * 0.69F - i * 0.74F) * impulse * 0.16F;
            }

            if (attack == DragonBossEntity.ATTACK_FIREBALL) {
                // Each shot sends a small recoil pulse down the spine.
                segment.xRot += fireballRecoil(attackTicks, i, 20.0F);
                segment.xRot += fireballRecoil(attackTicks, i, 32.0F);
                segment.xRot += fireballRecoil(attackTicks, i, 44.0F);
            }
        }
    }

    private static float attackSegmentScale(int attack, float attackTicks, float chainProgress) {
        if (attack == DragonBossEntity.ATTACK_RUSH && attackTicks >= 20.0F && attackTicks < 50.0F) {
            float localStraightening = ease((attackTicks - 20.0F - chainProgress * 7.0F) / 8.0F);
            float streamlined = 0.12F + chainProgress * 0.62F;
            return 1.0F + (streamlined - 1.0F) * localStraightening;
        }
        if (attack == DragonBossEntity.ATTACK_FIREBALL) {
            float aim = ease(attackTicks / 16.0F) * attackMotionWeight(attack, attackTicks);
            float anchored = 0.36F + chainProgress * 0.64F;
            return 1.0F + (anchored - 1.0F) * aim;
        }
        if (attack == DragonBossEntity.ATTACK_SONIC_BOOM) {
            float aim = ease(attackTicks / 30.0F) * attackMotionWeight(attack, attackTicks);
            float anchored = 0.20F + chainProgress * 0.80F;
            return 1.0F + (anchored - 1.0F) * aim;
        }
        if (attack == DragonBossEntity.ATTACK_LIGHTNING) {
            float charge = ease(attackTicks / 24.0F) * attackMotionWeight(attack, attackTicks);
            float anchored = 0.32F + chainProgress * 0.68F;
            return 1.0F + (anchored - 1.0F) * charge;
        }
        return 1.0F;
    }

    private static float attackMotionWeight(int attack, float attackTicks) {
        float duration;
        float feather;
        if (attack == DragonBossEntity.ATTACK_RUSH) {
            duration = DragonBossEntity.RUSH_DURATION;
            feather = 6.0F;
        } else if (attack == DragonBossEntity.ATTACK_COIL) {
            duration = DragonBossEntity.COIL_DURATION;
            feather = 8.0F;
        } else if (attack == DragonBossEntity.ATTACK_FIREBALL) {
            duration = DragonBossEntity.FIREBALL_DURATION;
            feather = 6.0F;
        } else if (attack == DragonBossEntity.ATTACK_SONIC_BOOM) {
            duration = DragonBossEntity.SONIC_BOOM_DURATION;
            feather = 8.0F;
        } else if (attack == DragonBossEntity.ATTACK_LIGHTNING) {
            duration = DragonBossEntity.LIGHTNING_DURATION;
            feather = 7.0F;
        } else {
            return 0.0F;
        }
        float fadeIn = ease(attackTicks / feather);
        float fadeOut = 1.0F - ease((attackTicks - (duration - feather)) / feather);
        return fadeIn * fadeOut;
    }

    private static float fireballRecoil(float attackTicks, int segmentIndex, float shotTick) {
        float age = attackTicks - shotTick;
        if (age < 0.0F || age > 15.0F) {
            return 0.0F;
        }
        float crest = age * 1.25F;
        float envelope = ease(1.0F - Math.abs(segmentIndex - crest) / 3.5F)
                * (1.0F - ease(age / 15.0F));
        return Mth.sin(age * 0.88F - segmentIndex * 0.67F) * envelope * 0.085F;
    }

    private void animateHead(DragonBossRenderState state) {
        float ticks = state.attackTicks;
        int attack = state.attackState;
        float lookWeight = attack == DragonBossEntity.ATTACK_RUSH ? 0.35F : 1.0F;
        if (attack == DragonBossEntity.ATTACK_SONIC_BOOM) {
            lookWeight = 1.15F;
        } else if (attack == DragonBossEntity.ATTACK_LIGHTNING) {
            lookWeight = 0.85F;
        }
        this.head.xRot += Mth.clamp(state.xRot, -40.0F, 40.0F) * DEG_TO_RAD * lookWeight;
        this.head.yRot += Mth.clamp(state.yRot, -50.0F, 50.0F) * DEG_TO_RAD * lookWeight;
        this.jaw.xRot += 0.045F + Mth.sin(state.ageInTicks * 0.13F) * 0.018F;

        if (attack == DragonBossEntity.ATTACK_RUSH) {
            float telegraph = ease(ticks / 20.0F);
            float charge = ticks < 20.0F ? 0.0F : ease((ticks - 20.0F) / 10.0F);
            this.head.xRot -= telegraph * 0.34F;
            this.head.xRot += charge * 0.22F;
            this.jaw.xRot += telegraph * 0.16F;
            this.maneCrown.xRot -= charge * 0.12F;
        } else if (attack == DragonBossEntity.ATTACK_COIL) {
            float tighten = ease(ticks / 60.0F);
            float charge = ticks >= 60.0F && ticks < 70.0F ? ease((ticks - 60.0F) / 10.0F) : 0.0F;
            float blast = ticks < 70.0F ? 0.0F : pulse(ticks, 72.0F, 8.0F);
            this.head.xRot -= tighten * 0.22F;
            this.head.yRot -= tighten * 0.24F;
            this.jaw.xRot += charge * 0.44F + blast * 0.82F;
            this.throat.xScale += charge * 0.20F;
            this.throat.yScale += charge * 0.26F;
            this.throat.zScale += charge * 0.20F;
        } else if (attack == DragonBossEntity.ATTACK_FIREBALL) {
            float windup = ease(ticks / 20.0F);
            float shot = Math.max(pulse(ticks, 20.0F, 3.5F), Math.max(pulse(ticks, 32.0F, 3.5F), pulse(ticks, 44.0F, 3.5F)));
            this.head.xRot -= windup * 0.31F;
            this.head.xRot += shot * 0.28F;
            this.jaw.xRot += 0.18F + windup * 0.36F + shot * 0.24F;
            this.throat.xScale += windup * 0.20F;
            this.throat.yScale += windup * 0.28F;
            this.throat.zScale += windup * 0.20F;
            this.beard.xRot += shot * 0.18F;
        } else if (attack == DragonBossEntity.ATTACK_SONIC_BOOM) {
            float aim = ease(ticks / 30.0F);
            float active = smoothWindow(ticks, 30.0F, 65.0F, 4.0F);
            this.head.xRot -= aim * 0.11F;
            this.head.yRot += Mth.sin(ticks * 1.9F) * 0.012F * active;
            this.jaw.xRot += aim * 0.32F + active * 0.24F;
            this.throat.xScale += aim * 0.14F + active * 0.10F;
            this.throat.yScale += aim * 0.18F + active * 0.12F;
            this.throat.zScale += aim * 0.14F + active * 0.10F;
            this.maneCrown.zRot += Mth.sin(ticks * 2.4F) * 0.018F * active;
        } else if (attack == DragonBossEntity.ATTACK_LIGHTNING) {
            float charge = ease(ticks / 24.0F) * attackMotionWeight(attack, ticks);
            float strike = pulse(ticks, 32.0F, 6.0F);
            this.head.xRot -= charge * 0.38F;
            this.head.zRot += Mth.sin(ticks * 2.8F) * charge * 0.018F;
            this.jaw.xRot += charge * 0.16F + strike * 0.30F;
            this.maneCrown.xRot -= charge * 0.14F;
            this.leftHorn.zRot += charge * 0.11F + strike * 0.08F;
            this.rightHorn.zRot -= charge * 0.11F + strike * 0.08F;
        }
    }

    private void animateWhiskers(DragonBossRenderState state) {
        float flare = 0.0F;
        if (state.attackState == DragonBossEntity.ATTACK_FIREBALL) {
            flare = ease(state.attackTicks / 20.0F);
        } else if (state.attackState == DragonBossEntity.ATTACK_SONIC_BOOM) {
            flare = ease(state.attackTicks / 30.0F);
        } else if (state.attackState == DragonBossEntity.ATTACK_LIGHTNING) {
            flare = ease(state.attackTicks / 24.0F)
                    * attackMotionWeight(state.attackState, state.attackTicks);
        } else if (state.attackState == DragonBossEntity.ATTACK_COIL && state.attackTicks >= 60.0F) {
            flare = 1.0F - ease((state.attackTicks - 70.0F) / 20.0F);
        }

        for (int sideIndex = 0; sideIndex < this.whiskers.length; sideIndex++) {
            float side = sideIndex == 0 ? 1.0F : -1.0F;
            for (int i = 0; i < this.whiskers[sideIndex].length; i++) {
                ModelPart whisker = this.whiskers[sideIndex][i];
                float phase = state.ageInTicks * 0.15F - i * 0.65F;
                whisker.yRot += side * Mth.sin(phase) * (0.055F + i * 0.018F);
                whisker.xRot += Mth.cos(phase * 0.78F) * 0.04F;
                whisker.yRot += side * flare * (0.08F + i * 0.035F);
                whisker.zRot -= side * flare * 0.055F;
            }
        }
    }

    private void animateLegs(DragonBossRenderState state) {
        float flightDrive = Mth.clamp(state.flightSpeed * 2.4F, 0.0F, 1.0F);
        for (int i = 0; i < this.upperLegs.length; i++) {
            float side = (i & 1) == 0 ? 1.0F : -1.0F;
            boolean front = i < 2;
            float rate = 0.145F + flightDrive * 0.055F + LEG_RATE_OFFSETS[i];
            float phase = state.ageInTicks * rate + LEG_PHASE_OFFSETS[i];
            float stroke = Mth.sin(phase);
            float recovery = 0.5F + 0.5F * Mth.sin(phase - 1.12F);
            float drift = Mth.sin(state.ageInTicks * (0.071F + i * 0.006F) + LEG_PHASE_OFFSETS[i] * 0.63F);

            // The upper limb paddles, the elbow/hock folds during recovery, and the paw
            // lags behind both.  Every limb has its own phase and slightly different rate.
            this.upperLegs[i].xRot += (front ? 0.26F : 0.18F)
                    + stroke * (front ? 0.30F : 0.24F)
                    + drift * 0.035F;
            this.upperLegs[i].yRot += side * (0.045F + drift * 0.028F);
            this.upperLegs[i].zRot += side * (0.045F + stroke * 0.055F)
                    + Mth.sin(phase * 0.47F + i) * 0.024F;

            this.lowerLegs[i].xRot += (front ? 0.18F : 0.31F)
                    - stroke * 0.13F
                    - recovery * (front ? 0.34F : 0.28F);
            this.lowerLegs[i].yRot -= side * drift * 0.035F;
            this.lowerLegs[i].zRot -= side * recovery * 0.055F;

            this.paws[i].xRot -= 0.12F + stroke * 0.10F - recovery * 0.18F;
            this.paws[i].yRot += side * (0.035F + Mth.sin(phase + 0.83F) * 0.045F);
            this.paws[i].zRot += side * drift * 0.045F;
        }

        if (state.attackState == DragonBossEntity.ATTACK_RUSH) {
            animateRushLegs(state.attackTicks);
        } else if (state.attackState == DragonBossEntity.ATTACK_COIL) {
            animateCoilLegs(state.attackTicks);
        } else if (state.attackState == DragonBossEntity.ATTACK_FIREBALL) {
            animateFireballLegs(state.attackTicks);
        } else if (state.attackState == DragonBossEntity.ATTACK_SONIC_BOOM) {
            animateSonicBoomLegs(state.attackTicks);
        } else if (state.attackState == DragonBossEntity.ATTACK_LIGHTNING) {
            animateLightningLegs(state.attackTicks);
        }
    }

    private void animateRushLegs(float ticks) {
        float anticipation = ticks < 20.0F ? ease(ticks / 20.0F) : 1.0F - ease((ticks - 20.0F) / 5.0F);
        float charge = ticks < 20.0F ? 0.0F : ease((ticks - 20.0F) / 7.0F);
        for (int i = 0; i < this.upperLegs.length; i++) {
            float side = (i & 1) == 0 ? 1.0F : -1.0F;
            boolean front = i < 2;
            float asymmetry = i == 0 ? 1.0F : i == 1 ? 0.88F : i == 2 ? 0.76F : 0.68F;

            // Forelegs reach for the target during the tell while the hind legs brace.
            this.upperLegs[i].xRot += anticipation * (front ? -0.72F * asymmetry : 0.42F * asymmetry);
            this.lowerLegs[i].xRot += anticipation * (front ? -0.24F : -0.48F);
            this.paws[i].xRot += anticipation * (front ? 0.34F : 0.23F);
            this.paws[i].yRot += side * anticipation * (front ? 0.15F : 0.08F);

            // Once the rush fires, all four limbs stream aft and fold close to the body.
            this.upperLegs[i].xRot += charge * (front ? 1.08F : 0.86F);
            this.upperLegs[i].zRot -= side * charge * 0.16F;
            this.lowerLegs[i].xRot -= charge * (front ? 1.28F : 1.08F);
            this.paws[i].xRot += charge * (0.43F + i * 0.025F);
            this.paws[i].zRot += side * charge * (0.07F + i * 0.012F);
        }
    }

    private void animateCoilLegs(float ticks) {
        float released = ticks < 70.0F ? 0.0F : ease((ticks - 70.0F) / 18.0F);
        float curl = ease(ticks / 45.0F) * (1.0F - released);
        for (int i = 0; i < this.upperLegs.length; i++) {
            float side = (i & 1) == 0 ? 1.0F : -1.0F;
            boolean front = i < 2;
            float clutch = Mth.sin(ticks * (0.105F + i * 0.007F) + LEG_PHASE_OFFSETS[i]) * 0.075F * curl;
            float fling = pulse(ticks, 70.0F + i * 0.85F, 7.0F);

            this.upperLegs[i].xRot += curl * (front ? 0.48F : 0.68F) + clutch;
            this.upperLegs[i].yRot += side * curl * (front ? -0.16F : 0.12F);
            this.upperLegs[i].zRot -= side * curl * (0.21F + i * 0.012F);
            this.lowerLegs[i].xRot -= curl * (front ? 0.92F : 1.06F) - clutch * 0.6F;
            this.paws[i].xRot += curl * (front ? 0.42F : 0.54F);
            this.paws[i].yRot += side * curl * 0.14F;

            // The shockwave opens the feet in a tiny travelling sequence rather than one snap.
            this.upperLegs[i].zRot += side * fling * 0.40F;
            this.lowerLegs[i].xRot += fling * 0.52F;
            this.paws[i].xRot -= fling * 0.38F;
        }
    }

    private void animateFireballLegs(float ticks) {
        float aim = ease(ticks / 20.0F);
        for (int i = 0; i < this.upperLegs.length; i++) {
            float side = (i & 1) == 0 ? 1.0F : -1.0F;
            boolean front = i < 2;
            float shot = Math.max(
                    pulse(ticks, 20.0F + i * 0.18F, 3.2F),
                    Math.max(pulse(ticks, 32.0F + i * 0.18F, 3.2F), pulse(ticks, 44.0F + i * 0.18F, 3.2F))
            );

            if (front) {
                // Uneven reaching foreclaws frame the muzzle and recoil with each shot.
                float reach = i == 0 ? 0.47F : 0.34F;
                this.upperLegs[i].xRot -= aim * reach;
                this.upperLegs[i].yRot += side * aim * (i == 0 ? 0.16F : 0.11F);
                this.lowerLegs[i].xRot -= aim * (i == 0 ? 0.25F : 0.16F);
                this.paws[i].xRot += aim * 0.32F;
                this.upperLegs[i].xRot += shot * (i == 0 ? 0.18F : 0.13F);
                this.paws[i].zRot += side * shot * 0.13F;
            } else {
                float fold = i == 2 ? 0.66F : 0.57F;
                this.upperLegs[i].xRot += aim * fold;
                this.upperLegs[i].zRot -= side * aim * 0.15F;
                this.lowerLegs[i].xRot -= aim * (fold + 0.34F);
                this.paws[i].xRot += aim * 0.44F + shot * 0.08F;
            }
        }
    }

    private void animateSonicBoomLegs(float ticks) {
        float aim = ease(ticks / 30.0F);
        float active = smoothWindow(ticks, 30.0F, 65.0F, 4.0F);
        for (int i = 0; i < this.upperLegs.length; i++) {
            float side = (i & 1) == 0 ? 1.0F : -1.0F;
            boolean front = i < 2;
            float tremor = Mth.sin(ticks * (0.72F + i * 0.055F) + LEG_PHASE_OFFSETS[i]) * active;
            if (front) {
                float reach = i == 0 ? 0.58F : 0.43F;
                this.upperLegs[i].xRot -= aim * reach;
                this.upperLegs[i].yRot += side * aim * (i == 0 ? 0.20F : 0.14F);
                this.lowerLegs[i].xRot -= aim * (i == 0 ? 0.31F : 0.22F);
                this.paws[i].xRot += aim * 0.39F;
                this.paws[i].yRot += side * aim * 0.12F;
            } else {
                float fold = i == 2 ? 0.72F : 0.62F;
                this.upperLegs[i].xRot += aim * fold;
                this.upperLegs[i].zRot -= side * aim * (i == 2 ? 0.19F : 0.15F);
                this.lowerLegs[i].xRot -= aim * (fold + 0.41F);
                this.paws[i].xRot += aim * 0.51F;
            }
            this.upperLegs[i].zRot += side * tremor * 0.018F;
            this.lowerLegs[i].yRot -= side * tremor * 0.022F;
            this.paws[i].zRot += side * tremor * 0.032F;
        }
    }

    private void animateLightningLegs(float ticks) {
        float charge = ease(ticks / 24.0F)
                * attackMotionWeight(DragonBossEntity.ATTACK_LIGHTNING, ticks);
        float strike = pulse(ticks, 32.0F, 6.0F);
        for (int i = 0; i < this.upperLegs.length; i++) {
            float side = (i & 1) == 0 ? 1.0F : -1.0F;
            boolean front = i < 2;
            float tremor = Mth.sin(ticks * (1.15F + i * 0.08F) + LEG_PHASE_OFFSETS[i]) * charge;
            this.upperLegs[i].xRot += charge * (front ? -0.34F : 0.48F);
            this.upperLegs[i].yRot += side * charge * (front ? 0.24F : -0.10F);
            this.upperLegs[i].zRot -= side * charge * (front ? 0.28F : 0.18F);
            this.lowerLegs[i].xRot -= charge * (front ? 0.42F : 0.74F);
            this.paws[i].xRot += charge * (front ? 0.30F : 0.40F);
            this.upperLegs[i].zRot += side * tremor * 0.025F;
            this.paws[i].zRot += side * (tremor * 0.04F + strike * 0.18F);
        }
    }

    private static String axialName(int index) {
        return "axial_" + (index < 10 ? "0" : "") + index;
    }

    private static float ease(float value) {
        float x = Mth.clamp(value, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static float pulse(float time, float center, float radius) {
        return ease(1.0F - Math.abs(time - center) / radius);
    }

    private static float smoothWindow(float time, float start, float end, float feather) {
        return ease((time - start) / feather) * (1.0F - ease((time - end) / feather));
    }
}
