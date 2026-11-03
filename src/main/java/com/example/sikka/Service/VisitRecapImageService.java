package com.example.sikka.Service;

import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitMember;
import com.example.sikka.Model.VisitRecord;
import com.example.sikka.Repository.VisitMemberRepository;
import com.example.sikka.Repository.VisitRecordRepository;
import com.example.sikka.Repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitRecapImageService {
    private final VisitRepository visitRepository;
    private final VisitMemberRepository visitMemberRepository;
    private final VisitRecordRepository visitRecordRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public byte[] createCompletedVisitImage(Integer visitId, Integer memberId) {
        Visit visit = visitRepository.findVisitById(visitId);
        VisitMember member = visitMemberRepository.findVisitMemberById(memberId);
        if (visit == null || member == null || !member.getVisit().getId().equals(visitId) || !"COMPLETED".equals(visit.getStatus())) throw new IllegalArgumentException("Completed visit record is required");
        List<VisitRecord> rides = visitRecordRepository.findAllByVisitMember_IdOrderByCreatedAtDescIdDesc(memberId).stream().filter(record -> "COMPLETED".equals(record.getStatus()) && record.getRide() != null).sorted(Comparator.comparing(VisitRecord::getCompletedAt, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        int rowSize = 5;
        int rows = Math.max(1, (rides.size() + rowSize - 1) / rowSize);
        int width = 1400;
        int height = 580 + rows * 255;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        Color blue = new Color(23, 58, 124);
        Color pale = new Color(235, 246, 252);
        Color red = new Color(223, 48, 55);
        Color text = new Color(32, 60, 93);
        g.setColor(pale);
        g.fillRoundRect(65, 55, 1270, 320, 52, 52);
        drawRightAligned(g, "سكة", width - 115, 144, 70, blue, true);
        drawRightAligned(g, "ملخص زيارتك", width - 115, 225, 44, blue, true);
        drawRightAligned(g, visit.getName() == null ? "يوم من ذكريات سكة" : visit.getName(), width - 115, 287, 26, text, false);
        drawRedTrain(g, 135, 128, red, blue);
        String date = visit.getVisitDate() == null ? "—" : visit.getVisitDate().toString();
        long minutes = visit.getStartedAt() != null && visit.getCompletedAt() != null ? Math.max(0, Duration.between(visit.getStartedAt(), visit.getCompletedAt()).toMinutes()) : 0;
        String duration = visit.getStartedAt() == null || visit.getCompletedAt() == null ? "غير متاح" : (minutes / 60) + " ساعة و" + (minutes % 60) + " دقيقة";
        drawStat(g, 90, 404, 370, "تاريخ الزيارة", date, blue, text);
        drawStat(g, 515, 404, 370, "الألعاب المكتملة", rides.size() + " ألعاب", blue, text);
        drawStat(g, 940, 404, 370, "مدة الزيارة", duration, blue, text);
        final int left = 140, right = 1260, top = 600, spread = 250;
        for (int row = 0; row < rows; row++) {
            int start = row * rowSize;
            int count = Math.min(rowSize, rides.size() - start);
            int y = top + row * spread;
            int firstX = row % 2 == 0 ? left : right;
            int finalX = row % 2 == 0 ? right : left;
            drawTrack(g, left, right, y, blue);
            if (row + 1 < rows) {
                int curveX = row % 2 == 0 ? right : left;
                g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(blue);
                g.draw(new Arc2D.Double(curveX - 110, y, 220, spread, row % 2 == 0 ? -90 : 90, 180, Arc2D.OPEN));
            }
            for (int col = 0; col < count; col++) {
                VisitRecord record = rides.get(start + col);
                int x = row % 2 == 0 ? firstX + col * (right - left) / Math.max(1, count - 1) : firstX - col * (right - left) / Math.max(1, count - 1);
                g.setColor(Color.WHITE);
                g.fillOval(x - 25, y - 25, 50, 50);
                g.setColor(red);
                g.setStroke(new BasicStroke(5f));
                g.drawOval(x - 25, y - 25, 50, 50);
                drawCentered(g, Integer.toString(start + col + 1), x, y + 9, 22, blue, true);
                String name = record.getRide().getName();
                if (name != null && name.length() > 24) name = name.substring(0, 23) + "…";
                drawCentered(g, name == null ? "لعبة" : name, x, y - 60, 19, text, true);
                String clock = record.getCompletedAt() == null ? "الوقت غير متاح" : record.getCompletedAt().toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
                drawCentered(g, clock, x, y + 70, 18, text, false);
            }
        }
        if (rides.isEmpty()) drawCentered(g, "ما فيه ألعاب مكتملة ومسجّلة في هذه الزيارة", width / 2, top, 28, blue, false);
        else {
            int lastRow = (rides.size() - 1) / rowSize;
            int count = rides.size() - lastRow * rowSize;
            int x = lastRow % 2 == 0 ? left + (count - 1) * (right - left) / Math.max(1, count - 1) : right - (count - 1) * (right - left) / Math.max(1, count - 1);
            drawRedTrain(g, Math.min(1190, Math.max(100, x - 55)), top + lastRow * spread - 95, red, blue);
        }
        drawCentered(g, "سكتك، ذكريات تستاهل تنحفظ 🚂", width / 2, height - 65, 25, blue, true);
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to render visit recap PNG", exception);
        }
    }

    private void drawStat(Graphics2D g, int x, int y, int width, String label, String value, Color blue, Color text) {
        g.setColor(new Color(248, 251, 254));
        g.fillRoundRect(x, y, width, 112, 24, 24);
        drawCentered(g, label, x + width / 2, y + 37, 19, text, false);
        drawCentered(g, value, x + width / 2, y + 83, 25, blue, true);
    }

    private void drawTrack(Graphics2D g, int left, int right, int y, Color blue) {
        g.setColor(blue);
        g.setStroke(new BasicStroke(6f));
        g.drawLine(left, y - 10, right, y - 10);
        g.drawLine(left, y + 10, right, y + 10);
        g.setStroke(new BasicStroke(3f));
        for (int x = left; x <= right; x += 24) g.drawLine(x, y - 20, x, y + 20);
    }

    private void drawRedTrain(Graphics2D g, int x, int y, Color red, Color blue) {
        g.setColor(red);
        g.fillRoundRect(x, y, 75, 47, 16, 16);
        g.fillRoundRect(x + 86, y, 75, 47, 16, 16);
        g.setColor(new Color(213, 240, 252));
        for (int offset : new int[]{17, 103}) g.fillRoundRect(x + offset, y + 9, 37, 19, 6, 6);
        g.setColor(blue);
        for (int offset : new int[]{17, 58, 103, 144}) g.fillOval(x + offset - 7, y + 41, 16, 16);
        g.setStroke(new BasicStroke(5f));
        g.drawLine(x + 74, y + 35, x + 87, y + 35);
    }

    private void drawCentered(Graphics2D g, String value, int x, int y, int size, Color color, boolean bold) {
        g.setColor(color);
        Font font = new Font("Dialog", bold ? Font.BOLD : Font.PLAIN, size);
        TextLayout text = new TextLayout(value, font, g.getFontRenderContext());
        text.draw(g, x - text.getAdvance() / 2f, y);
    }

    private void drawRightAligned(Graphics2D g, String value, int right, int baseline, int size, Color color, boolean bold) {
        g.setColor(color);
        Font font = new Font("Dialog", bold ? Font.BOLD : Font.PLAIN, size);
        TextLayout text = new TextLayout(value, font, g.getFontRenderContext());
        text.draw(g, right - text.getAdvance(), baseline);
    }
}
