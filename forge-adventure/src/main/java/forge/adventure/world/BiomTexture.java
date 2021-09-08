package forge.adventure.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import forge.adventure.data.BiomData;
import forge.adventure.data.BiomTerrainData;
import forge.adventure.util.Config;

import java.io.IOException;
import java.io.Serializable;

public class BiomTexture implements Serializable {
    private final BiomData data;
    private final int tileSize;
    public Pixmap emptyPixmap = new Pixmap(1, 1, Pixmap.Format.RGB888);
    Array<Array<Pixmap>> images = new Array<>();
    Array<Array<Pixmap>> smallImages = new Array<>();
    Array<IntMap<Pixmap>> edgeImages = new Array<>();

    public BiomTexture(BiomData data,int tileSize) {
        this.data = data;
        this.tileSize=tileSize;
        generate();


    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {

    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        generate();

    }

    private void generate() {
        Pixmap completePicture = null;

        if (images != null) {
            for (Array<Pixmap> val : images) {

                for (Pixmap img : val) {
                    img.dispose();
                }
            }
            images.clear();
        }
        images = new Array<>();
        if (smallImages != null) {
            for (Array<Pixmap> val : smallImages) {

                for (Pixmap img : val) {
                    img.dispose();
                }
            }
            smallImages.clear();
        }
        smallImages = new Array<>();
        if (edgeImages != null) {
            for (IntMap<Pixmap> val : edgeImages) {

                for (IntMap.Entry<Pixmap> img : val) {
                    img.value.dispose();
                }
            }
            edgeImages.clear();
        }
        edgeImages = new Array<>();

        Array<TextureAtlas.AtlasRegion> regions =new Array<>();
        regions.add(Config.instance().getAtlas(data.tilesetAtlas).findRegion(data.tilesetName));
        if(data.terrain!=null)
        {
            for(BiomTerrainData terrain:data.terrain)
            {
                regions.add(Config.instance().getAtlas(data.tilesetAtlas).findRegion(terrain.spriteName));
            }
        }
        for (TextureAtlas.AtlasRegion region : regions) {
            Array<Pixmap> pics = new Array<Pixmap>();
            Array<Pixmap> spics = new Array<Pixmap>();
            if (completePicture == null) {
                region.getTexture().getTextureData().prepare();
                completePicture = region.getTexture().getTextureData().consumePixmap();
            }

            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 3; x++) {
                    int px = region.getRegionX() + (x * tileSize);
                    int py = region.getRegionY() + (y * tileSize);
                    Pixmap subPixmap = new Pixmap(tileSize, tileSize, Pixmap.Format.RGBA8888);
                    subPixmap.drawPixmap(completePicture, 0, 0, px, py, tileSize, tileSize);
                    pics.add(subPixmap);
                }
            }
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 6; x++) {
                    int px = region.getRegionX() + (x * tileSize / 2);
                    int py = region.getRegionY() + (y * tileSize / 2);
                    Pixmap subPixmap = new Pixmap(tileSize / 2, tileSize / 2, Pixmap.Format.RGBA8888);
                    subPixmap.drawPixmap(completePicture, 0, 0, px, py, tileSize / 2, tileSize / 2);
                    spics.add(subPixmap);
                }
            }
            images.add(pics);
            smallImages.add(spics);
            edgeImages.add(new IntMap<>());

        }
    }

    public Pixmap getPixmap(int biomSubIndex) {
        if (biomSubIndex >= edgeImages.size || biomSubIndex < 0) {
            return emptyPixmap;
        }
        return images.get(biomSubIndex).get(BigPictures.Center.value);
    }

    public Pixmap drawPixmapOn(int biomSubIndex, int neighbors, Pixmap subPixmap) {

        int id = (neighbors * 100);
        if (biomSubIndex >= edgeImages.size || biomSubIndex < 0) {
            return emptyPixmap;
        }
        if (edgeImages.get(biomSubIndex).containsKey(id))
            return edgeImages.get(biomSubIndex).get(id);
        int tileSize = subPixmap.getHeight();
        switch (neighbors) {
            case 0b111_111_111:
                subPixmap.drawPixmap(images.get(biomSubIndex).get(BigPictures.Center.value), 0, 0);
                break;
            case 0b111_111_000://bot is missing
                subPixmap.drawPixmap(images.get(biomSubIndex).get(BigPictures.BottomEdge.value), 0, 0);
                break;
            case 0b000_111_111://top is missing
                subPixmap.drawPixmap(images.get(biomSubIndex).get(BigPictures.TopEdge.value), 0, 0);
                break;
            case 0b011_011_011://left is missing
                subPixmap.drawPixmap(images.get(biomSubIndex).get(BigPictures.LeftEdge.value), 0, 0);
                break;
            case 0b110_110_110://right is missing
                subPixmap.drawPixmap(images.get(biomSubIndex).get(BigPictures.RightEdge.value), 0, 0);
                break;
            case 0b010_111_010://cross
                subPixmap.drawPixmap(images.get(biomSubIndex).get(BigPictures.InnerEdges.value), 0, 0);
                break;
            case 0b001_011_111://top Left
                subPixmap.drawPixmap(images.get(biomSubIndex).get(BigPictures.LeftTopEdge.value), 0, 0);
                break;
            case 0b100_110_111://top Right
                subPixmap.drawPixmap(images.get(biomSubIndex).get(BigPictures.RightTopEdge.value), 0, 0);
                break;
            case 0b111_011_001://bottom Left
                subPixmap.drawPixmap(images.get(biomSubIndex).get(BigPictures.LeftBottomEdge.value), 0, 0);
                break;
            case 0b111_110_100://bottom Right
                subPixmap.drawPixmap(images.get(biomSubIndex).get(BigPictures.RigtBottomEdge.value), 0, 0);
                break;
            default: {
                switch (neighbors & 0b110_100_000)//topLeftNeighbors
                {
                    case 0b000_000_000:
                    case 0b100_000_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.LeftTopEdge00.value), 0, 0);
                        break;
                    case 0b010_000_000:
                    case 0b110_000_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.LeftEdge00.value), 0, 0);
                        break;
                    case 0b000_100_000:
                    case 0b100_100_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.TopEdge00.value), 0, 0);
                        break;
                    case 0b010_100_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.InnerTopLeftEdge.value), 0, 0);
                        break;
                    case 0b110_100_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.Center00.value), 0, 0);
                        break;
                }

                switch (neighbors & 0b011_001_000)//topRightNeighbors
                {
                    case 0b000_000_000:
                    case 0b001_000_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.RightTopEdge10.value), tileSize / 2, 0);
                        break;
                    case 0b011_000_000:
                    case 0b010_000_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.RightEdge10.value), tileSize / 2, 0);
                        break;
                    case 0b001_001_000:
                    case 0b000_001_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.TopEdge10.value), tileSize / 2, 0);
                        break;
                    case 0b010_001_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.InnerTopRightEdge.value), tileSize / 2, 0);
                        break;
                    case 0b011_001_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.Center10.value), tileSize / 2, 0);
                        break;
                }
                switch (neighbors & 0b000_100_110) {//bottomLeftNeighbors
                    case 0b000_000_000:
                    case 0b000_000_100:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.LeftBottomEdge01.value), 0, tileSize / 2);
                        break;
                    case 0b000_100_100:
                    case 0b000_100_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.BottomEdge01.value), 0, tileSize / 2);
                        break;
                    case 0b000_000_110:
                    case 0b000_000_010:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.LeftEdge01.value), 0, tileSize / 2);
                        break;
                    case 0b000_100_010:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.InnerBottomLeftEdge.value), 0, tileSize / 2);
                        break;
                    case 0b000_100_110:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.Center01.value), 0, tileSize / 2);
                        break;
                }
                switch (neighbors & 0b000_001_011) {//bottomRightNeighbors
                    case 0b000_000_000:
                    case 0b000_000_001:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.RightBottomEdge11.value), tileSize / 2, tileSize / 2);
                        break;
                    case 0b000_001_001:
                    case 0b000_001_000:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.BottomEdge11.value), tileSize / 2, tileSize / 2);
                        break;
                    case 0b000_000_011:
                    case 0b000_000_010:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.RightEdge11.value), tileSize / 2, tileSize / 2);
                        break;
                    case 0b000_001_010:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.InnerBottomRightEdge.value), tileSize / 2, tileSize / 2);
                        break;
                    case 0b000_001_011:
                        subPixmap.drawPixmap(smallImages.get(biomSubIndex).get(SmallPictures.Center11.value), tileSize / 2, tileSize / 2);
                        break;
                }
            }
        }
        if (false)//debug neighbors
        {
            subPixmap.setColor(Color.GREEN);
            subPixmap.drawLine(1, 1, tileSize - 1, 1);
            subPixmap.drawLine(1, 1, 1, tileSize - 1);
            subPixmap.drawLine(tileSize - 1, 1, tileSize - 1, tileSize - 1);
            subPixmap.drawLine(1, tileSize - 1, tileSize - 1, tileSize - 1);
            subPixmap.setColor(new Color(1, 0, 0, 0.1f));
            if ((neighbors & 0b100_000_000) > 0) {
                subPixmap.fillRectangle(0, 0, tileSize / 3, tileSize / 3);
            }
            if ((neighbors & 0b010_000_000) > 0) {
                subPixmap.fillRectangle(tileSize / 3, 0, tileSize / 3, tileSize / 3);
            }
            if ((neighbors & 0b001_000_000) > 0) {
                subPixmap.fillRectangle((tileSize / 3) * 2, 0, tileSize / 3, tileSize / 3);
            }
            if ((neighbors & 0b000_100_000) > 0) {
                subPixmap.fillRectangle(0, tileSize / 3, tileSize / 3, tileSize / 3);
            }
            if ((neighbors & 0b000_010_000) > 0) {
                subPixmap.fillRectangle(tileSize / 3, tileSize / 3, tileSize / 3, tileSize / 3);
            }
            if ((neighbors & 0b000_001_000) > 0) {
                subPixmap.fillRectangle((tileSize / 3) * 2, tileSize / 3, tileSize / 3, tileSize / 3);
            }
            if ((neighbors & 0b000_000_100) > 0) {
                subPixmap.fillRectangle(0, (tileSize / 3) * 2, tileSize / 3, tileSize / 3);
            }
            if ((neighbors & 0b000_000_010) > 0) {
                subPixmap.fillRectangle(tileSize / 3, (tileSize / 3) * 2, tileSize / 3, tileSize / 3);
            }
            if ((neighbors & 0b000_000_001) > 0) {
                subPixmap.fillRectangle((tileSize / 3) * 2, (tileSize / 3) * 2, tileSize / 3, tileSize / 3);
            }

        }
        edgeImages.get(biomSubIndex).put(biomSubIndex, subPixmap);
        return subPixmap;

    }

    enum BigPictures {
        Empty1(0),
        Empty2(1),
        InnerEdges(2),
        LeftTopEdge(3),
        TopEdge(4),
        RightTopEdge(5),
        LeftEdge(6),
        Center(7),
        RightEdge(8),
        LeftBottomEdge(9),
        BottomEdge(10),
        RigtBottomEdge(11);

        public int value;

        BigPictures(int i) {
            value = i;
        }
    }


    enum SmallPictures {
        Empty1(0),
        Empty2(1),
        Empty3(2),
        Empty4(3),
        InnerTopLeftEdge(4),
        InnerTopRightEdge(5),
        Empty5(6),
        Empty6(7),
        Empty7(8),
        Empty8(9),
        InnerBottomLeftEdge(10),
        InnerBottomRightEdge(11),
        LeftTopEdge00(12),
        LeftTopEdge10(13),
        TopEdge00(14),
        TopEdge10(15),
        RightTopEdge00(16),
        RightTopEdge10(17),
        LeftTopEdge01(18),
        LeftTopEdge11(19),
        TopEdge01(20),
        TopEdge11(21),
        RightTopEdge01(22),
        RightTopEdge11(23),
        LeftEdge00(24),
        LeftEdge10(25),
        Center00(26),
        Center10(27),
        RightEdge00(28),
        RightEdge10(29),
        LeftEdge01(30),
        LeftEdge11(31),
        Center01(32),
        Center11(33),
        RightEdge01(34),
        RightEdge11(35),
        LeftBottomEdge00(36),
        LeftBottomEdge10(37),
        BottomEdge00(38),
        BottomEdge10(39),
        RightBottomEdge00(40),
        RightBottomEdge10(41),
        LeftBottomEdge01(42),
        LeftBottomEdge11(43),
        BottomEdge01(44),
        BottomEdge11(45),
        RightBottomEdge01(46),
        RightBottomEdge11(47);

        public int value;

        SmallPictures(int i) {
            value = i;
        }
    }
}
