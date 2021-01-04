import { h } from "preact";
import { PSCTreeLayoutNode } from "../../../../types/calculus/psc";
import { SelectNodeOptions } from "../../../../types/calculus/tableaux";
import { selected } from "../../../svg/rectangle/style.scss";
import { PSCNode } from "../../../../types/calculus/psc";
import { PSCState } from "../../../../types/calculus/psc";
import Zoomable from "../../../svg/zoomable";
import { findSubTree } from "../../../../util/layout/tree";
import { pscTreeLayout } from "../../../../util/psc";
import * as style from "./style.scss";

interface Props {
    // Nodes of the tree
    nodes: PSCNode[];
    // The id of the selected Node
    selectedNodeId: number | undefined;
    // The function to call, when user selects a node
    selectedNodeCallback: (
        node: PSCTreeLayoutNode,
        options?: SelectNodeOptions,
    ) => void;
    // informs the element if the screen is small
    smallScreen: boolean;
}

const PSCTreeView: preact.FunctionalComponent<Props> = ({
    nodes,
    selectedNodeCallback,
    selectedNodeId,
}) => {
    const { root, height, width: treeWidth } = pscTreeLayout(nodes);

    const treeHeight = Math.max(height, 200);

    const transformGoTo = (d: any): [number, number] => {
        const n = d.node as number;

        const node = findSubTree(
            root,
            (t) => t.data.id === selectedNodeId,
            (t) => t,
        )!;
        selectedNodeCallback(node.data,{ignoreClause:true});

        const{x,y}=node;
        return [treeWidth/2-x,treeHeight/2-y];

       
    };

    return(
        <div class="card">
            {/* <Zoomable
                class={style.svg}
                width="100%"
                height="calc(100vh - 192px)"
                style="min-height: 60vh"
                viewBox={`0 -16 ${treeWidth} ${treeHeight}`}
                preserveAspectRatio="xMidYMid meet"
                transformGoTo={transformGoTo}
            >
                {(transform) => (<g transform={`translate(${transform.x} ${transform.y}) scale(${transform.k})`}></g>)}
            </Zoomable> */}
        </div>
        
    );
};

export default PSCTreeView;
