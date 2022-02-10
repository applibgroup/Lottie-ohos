package com.airbnb.lottie.animation.content;

import com.airbnb.lottie.model.content.MergePaths;
import ohos.agp.render.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class MergePathsContent implements PathContent, GreedyContent {
    private final Path firstPath = new Path();
    private final Path remainderPath = new Path();
    private final Path path = new Path();

    private final String name;
    private final List<PathContent> pathContents = new ArrayList<>();
    private final MergePaths mergePaths;

    public MergePathsContent(MergePaths mergePaths) {
        name = mergePaths.getName();
        this.mergePaths = mergePaths;
    }

    @Override
    public void absorbContent(ListIterator<Content> contents) {
        // Fast forward the iterator until after this content.
        while (contents.hasPrevious() && contents.previous() != this) {}
        while (contents.hasPrevious()) {
            Content content = contents.previous();
            if (content instanceof PathContent) {
                pathContents.add((PathContent) content);
                contents.remove();
            }
        }
    }

    @Override
    public void setContents(List<Content> contentsBefore, List<Content> contentsAfter) {
        for (int i = 0; i < pathContents.size(); i++) {
            pathContents.get(i).setContents(contentsBefore, contentsAfter);
        }
    }

    @Override
    public Path getPath() {
        path.reset();

        if (mergePaths.isHidden()) {
            return path;
        }
        // TODO : enum Path.Op not supported in HMOS
        switch (mergePaths.getMode()) {
            case MERGE:
                addPaths();
                break;
        }
        return path;
    }

    @Override
    public String getName() {
        return name;
    }

    private void addPaths() {
        for (int i = 0; i < pathContents.size(); i++) {
            path.addPath(pathContents.get(i).getPath());
        }
    }


}
