package com.digdes.school.node;


import java.util.Map;

public abstract class Node {
    private Node left,right;


    public Node(Node left, Node right) {
        this.left = left;
        this.right = right;
    }

    public abstract boolean compute(Map<String,Object> row);

    public Node getLeft() {
        return left;
    }

    public Node getRight() {
        return right;
    }
}
