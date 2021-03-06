import { h, RefObject } from "preact";
import { useEffect, useRef, useState } from "preact/hooks";

import {
    FormulaTreeLayoutNode,
    SequentTreeLayoutNode,
} from "../../../../types/calculus/sequent";
import { LayoutItem } from "../../../../types/layout";
import { classMap } from "../../../../util/class-map";
import { nodeName } from "../../../../util/sequent";
import HorizontalList from "../../../svg/horizontalList";
import Rectangle from "../../../svg/rectangle";

import * as style from "./style.scss";

interface Props {
    /**
     * The single tree node to represent
     */
    node: LayoutItem<SequentTreeLayoutNode>;

    parent?: LayoutItem<SequentTreeLayoutNode>;

    selected: boolean;

    selectNodeCallback: (
        node: SequentTreeLayoutNode,
        selectValue?: boolean,
    ) => void;

    selectedListIndex?: string;

    selectFormulaCallback: (formula: FormulaTreeLayoutNode) => void;
}

const lineUnderNode = (
    node: LayoutItem<SequentTreeLayoutNode>,
    parent: LayoutItem<SequentTreeLayoutNode> | undefined,
    textRef: RefObject<SVGTextElement>,
    ruleName: string | undefined,
) => {
    if (parent !== undefined) {
        const [parentDims, setParentDims] = useState({
            x: 0,
            y: 0,
            height: 0,
            width: 0,
        });
        const [dims, setDims] = useState({ x: 0, y: 0, height: 0, width: 0 });

        const parentTextRef = useRef<SVGTextElement>();

        useEffect(() => {
            if (!parentTextRef.current) {
                return;
            }

            const parentBox = parentTextRef.current.getBBox();
            parentBox.height += 8;
            parentBox.y -= 4;
            parentBox.width += 16;
            parentBox.x -= 8;
            setParentDims(parentBox);

            if (!textRef.current) {
                return;
            }

            const box = textRef.current.getBBox();
            box.height += 8;
            box.y -= 4;
            box.width += 16;
            box.x -= 8;
            setDims(box);
        });

        let lastMove: string = "";
 
        switch (ruleName) {
            case "notRight": 
                lastMove = "¬R"; break;
            case "notLeft": 
                lastMove = "¬L"; break;
            case "andRight": 
                lastMove = "∧R"; break;
            case "andLeft": 
                lastMove = "∧L"; break;
            case "orRight": 
                lastMove = "∨R"; break;
            case "orLeft": 
                lastMove = "∨L"; break;
            case "impLeft": 
                lastMove = "→L"; break;
            case "impRight": 
                lastMove = "→R"; break;
            case "Ax": 
                lastMove = "Ax"; break;
            case "exLeft": 
                lastMove = "∃L"; break;
            case "exRight": 
                lastMove = "∃R"; break;
            case "allLeft": 
                lastMove = "∀L"; break;
            case "allRight": 
                lastMove = "∀R"; break;
            default: lastMove = "";
        }

        const parentWidth = parentDims.width;
        const width = dims.width;

        if (node.x < parent.x) {
            return (
                <g>
                    <text
                        ref={parentTextRef}
                        class={style.trans}
                        text-anchor="middle"
                        x={parent.x}
                        y={parent.y}
                    >
                        {nodeName(parent.data)}
                    </text>
                    <line
                        class={style.link}
                        x1={Math.min(
                            parent.x - parentWidth / 2,
                            node.x - width / 2,
                        )}
                        y1={node.y + 15}
                        x2={Math.max(
                            parent.x + parentWidth / 2,
                            node.x + width / 2,
                        )}
                        y2={node.y + 15}
                    />
                </g>
            );
        }

        return (
            <g>
                <text
                    ref={parentTextRef}
                    class={style.trans}
                    text-anchor="middle"
                    x={parent.x}
                    y={parent.y}
                >
                    {nodeName(parent.data)}
                </text>
                <line
                    class={style.link}
                    x1={Math.min(
                        parent.x - parentWidth / 2,
                        node.x - width / 2,
                    )}
                    y1={node.y + 15}
                    x2={Math.max(
                        parent.x + parentWidth / 2,
                        node.x + width / 2,
                    )}
                    y2={node.y + 15}
                />
                <text
                    ref={textRef}
                    class={style.lineText}
                    text-anchor="middle"
                    x={
                        Math.max(
                            node.x + width / 2,
                            parent.x + parentWidth / 2,
                        ) + 15
                    }
                    y={node.y + 17}
                >
                    {lastMove}
                </text>
            </g>
        );
    }
    return;
};

const SequentTreeNode: preact.FunctionalComponent<Props> = ({
    node,
    parent,
    selected,
    selectedListIndex,
    selectNodeCallback,
    selectFormulaCallback,
}) => {
    const textRef = useRef<SVGTextElement>();

    // Uses parameter lemmaNodesSelectable to determine if the Node should be selectable
    const nodeIsClickable = !node.data.isClosed;

    /**
     * Handle the onClick event of the node
     * @returns {void}
     */
    const handleClick = () => {
        if (nodeIsClickable) {
            selectNodeCallback(node.data);
        }
    };

    if (
        node.data.leftFormulas.length === 0 &&
        node.data.rightFormulas.length === 0 &&
        node.data.isClosed
    ) {
        return (
            <g>
                <text
                    ref={textRef}
                    class={style.trans}
                    text-anchor="middle"
                    x={node.x}
                    y={node.y}
                >
                    {nodeName(node.data)}
                </text>
                <g>
                    {lineUnderNode(
                        node,
                        parent,
                        textRef,
                        node.data.lastMove?.type,
                    )}
                </g>
            </g>
        );
    }

    return (
        <g onClick={handleClick}>
            <text
                ref={textRef}
                class={style.trans}
                text-anchor="middle"
                x={node.x}
                y={node.y}
            >
                {nodeName(node.data)}
            </text>
            <Rectangle
                elementRef={textRef}
                disabled={node.data.isClosed}
                selected={selected}
                class={classMap({
                    [style.unselected]: !selected && !node.data.isClosed,
                    [style.nodeClickable]: !node.data.isClosed,
                    [style.node]: !selected,
                    [style.rectSelected]: selected,
                })}
            />
            <g>
                <HorizontalList
                    textRef={textRef}
                    node={node}
                    leftFormulas={node.data.leftFormulas}
                    rightFormulas={node.data.rightFormulas}
                    selected={selected}
                    selectNodeCallback={selectNodeCallback}
                    selectFormulaCallback={selectFormulaCallback}
                    selectedListIndex={selectedListIndex}
                />
                <g
                    onClick={() => {
                        event?.stopPropagation();
                    }}
                >
                    {lineUnderNode(
                        node,
                        parent,
                        textRef,
                        node.data.lastMove?.type,
                    )}
                </g>
            </g>
        </g>
    );
};

export default SequentTreeNode;
