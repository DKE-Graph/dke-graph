import time
import torch
from torch_geometric.datasets import Planetoid
import torch_geometric.transforms as T
import torch.nn as nn
from torch_geometric.nn import GATConv
import torch.nn.functional as F

start_time = time.time()
device = "cpu"

# Data
dataset = Planetoid(root='./', name='Cora')
graph = dataset[0]
split = T.RandomNodeSplit(num_val=0.1, num_test=0.2)
graph = split(graph)
#graph.to(device)

# Model
class GATNet(torch.nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = GATConv(dataset.num_node_features, 8, heads=8, dropout=0.6)
        self.conv2 = GATConv(8*8, dataset.num_classes, heads=1, dropout=0.6)

    def forward(self, data):
        x, edge_index = data.x, data.edge_index

        x = F.dropout(x, p=0.6, training=self.training)
        x = F.elu(self.conv1(x, edge_index))
        x = F.dropout(x, p=0.6, training=self.training)
        x = self.conv2(x, edge_index)
        return F.log_softmax(x, dim=1)

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

gat = GATNet().to(device)
optimizer_gat = torch.optim.Adam(gat.parameters(), lr=0.005, weight_decay=5e-4)
criterion = nn.CrossEntropyLoss()
gat = train_node_classifier(gat, graph, optimizer_gat, criterion)

test_acc = eval_node_classifier(gat, graph, graph.test_mask)
print(f'Test Acc: {test_acc:.3f}')
end_time = time.time()
print("총 소요 시간: %.3f초" %(end_time - start_time))
