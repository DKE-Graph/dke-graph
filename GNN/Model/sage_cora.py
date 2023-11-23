import time
import torch
from torch_geometric.datasets import Planetoid
import torch_geometric.transforms as T
import torch.nn as nn
from torch_geometric.nn import SAGEConv
import torch.nn.functional as F
#from torch_scatter import scatter_mean

start_time = time.time()
device = "cpu"
# Data
dataset = Planetoid(root='./', name='Cora')
graph = dataset[0]
split = T.RandomNodeSplit(num_val=0.1, num_test=0.2)
graph = split(graph)
#graph.to(device)

# Model
class GraphSAGE(torch.nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = SAGEConv(dataset.num_node_features, 16, aggr='max')
        self.conv2 = SAGEConv(16, dataset.num_classes, aggr='max')

    def forward(self, data):
        x, edge_index = data.x, data.edge_index

        x = F.relu(self.conv1(x, edge_index))
        output = self.conv2(x, edge_index)
        return output

# 학습 함수
def train_node_classifier(model, graph, optimizer, criterion, n_epochs=200):
    for epoch in range(1, n_epochs + 1):
        model.train()
        optimizer.zero_grad()
        out = model(graph)
        loss = criterion(out[graph.train_mask], graph.y[graph.train_mask])
        loss.backward()
        optimizer.step()

        pred = out.argmax(dim=1)
        acc = eval_node_classifier(model, graph, graph.val_mask)

        if epoch % 10 == 0:
            print(f'Epoch: {epoch:03d}, Train Loss: {loss:.3f}, Val Acc: {acc:.3f}')

    return model

# 검증 함수
def eval_node_classifier(model, graph, mask):

    model.eval()
    pred = model(graph).argmax(dim=1)
    correct = (pred[mask] == graph.y[mask]).sum()
    acc = int(correct) / int(mask.sum())

    return acc

sage = GraphSAGE().to(device)
optimizer_sage = torch.optim.Adam(sage.parameters(), lr=0.01, weight_decay=5e-4)
criterion = nn.CrossEntropyLoss()
sage = train_node_classifier(sage, graph, optimizer_sage, criterion)

test_acc = eval_node_classifier(sage, graph, graph.test_mask)
print(f'Test Acc: {test_acc:.3f}')
end_time = time.time()
print("총 소요 시간: %.3f초" %(end_time - start_time))
